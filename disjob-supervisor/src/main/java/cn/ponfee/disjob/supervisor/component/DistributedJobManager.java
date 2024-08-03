/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.supervisor.component;

import cn.ponfee.disjob.common.base.IdGenerator;
import cn.ponfee.disjob.common.base.LazyLoader;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.dag.DAGEdge;
import cn.ponfee.disjob.common.dag.DAGNode;
import cn.ponfee.disjob.common.model.BaseEntity;
import cn.ponfee.disjob.common.spring.TransactionUtils;
import cn.ponfee.disjob.common.tuple.Tuple2;
import cn.ponfee.disjob.common.tuple.Tuple3;
import cn.ponfee.disjob.common.util.Strings;
import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.base.WorkerRpcService;
import cn.ponfee.disjob.core.dag.PredecessorInstance;
import cn.ponfee.disjob.core.dto.supervisor.StartTaskParam;
import cn.ponfee.disjob.core.dto.supervisor.StartTaskResult;
import cn.ponfee.disjob.core.dto.supervisor.StopTaskParam;
import cn.ponfee.disjob.core.dto.worker.SplitJobParam;
import cn.ponfee.disjob.core.enums.*;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.model.*;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import cn.ponfee.disjob.dispatch.TaskDispatcher;
import cn.ponfee.disjob.dispatch.event.TaskDispatchFailedEvent;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.registry.rpc.DestinationServerRestProxy.DestinationServerInvoker;
import cn.ponfee.disjob.registry.rpc.DiscoveryServerRestProxy.GroupedServerInvoker;
import cn.ponfee.disjob.supervisor.configuration.SupervisorProperties;
import cn.ponfee.disjob.supervisor.dag.WorkflowGraph;
import cn.ponfee.disjob.supervisor.dao.mapper.*;
import cn.ponfee.disjob.supervisor.instance.GeneralInstanceCreator;
import cn.ponfee.disjob.supervisor.instance.TriggerInstance;
import cn.ponfee.disjob.supervisor.instance.TriggerInstanceCreator;
import cn.ponfee.disjob.supervisor.instance.WorkflowInstanceCreator;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.LongFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable.doCaught;
import static cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable.doChecked;
import static cn.ponfee.disjob.common.exception.Throwables.ThrowingSupplier.doCaught;
import static cn.ponfee.disjob.common.spring.TransactionUtils.*;
import static cn.ponfee.disjob.common.util.Functions.convert;
import static cn.ponfee.disjob.common.util.Functions.doIfTrue;
import static cn.ponfee.disjob.core.base.JobConstants.PROCESS_BATCH_SIZE;
import static cn.ponfee.disjob.supervisor.dao.SupervisorDataSourceConfig.SPRING_BEAN_NAME_TX_MANAGER;
import static cn.ponfee.disjob.supervisor.dao.SupervisorDataSourceConfig.SPRING_BEAN_NAME_TX_TEMPLATE;

/**
 * Manage distributed schedule job.
 *
 * @author Ponfee
 */
@Component
public class DistributedJobManager extends AbstractJobManager {

    private static final List<Integer> RS_TERMINABLE = Collects.convert(RunState.Const.TERMINABLE_LIST, RunState::value);
    private static final List<Integer> RS_RUNNABLE = Collects.convert(RunState.Const.RUNNABLE_LIST, RunState::value);
    private static final List<Integer> RS_PAUSABLE = Collects.convert(RunState.Const.PAUSABLE_LIST, RunState::value);
    private static final List<Integer> RS_WAITING = Collections.singletonList(RunState.WAITING.value());
    private static final List<Integer> RS_RUNNING = Collections.singletonList(RunState.RUNNING.value());
    private static final List<Integer> RS_PAUSED = Collections.singletonList(RunState.PAUSED.value());

    private static final List<Integer> ES_EXECUTABLE = Collects.convert(ExecuteState.Const.EXECUTABLE_LIST, ExecuteState::value);
    private static final List<Integer> ES_PAUSABLE = Collects.convert(ExecuteState.Const.PAUSABLE_LIST, ExecuteState::value);
    private static final List<Integer> ES_WAITING = Collections.singletonList(ExecuteState.WAITING.value());
    private static final List<Integer> ES_EXECUTING = Collections.singletonList(ExecuteState.EXECUTING.value());
    private static final List<Integer> ES_PAUSED = Collections.singletonList(ExecuteState.PAUSED.value());

    private final SchedInstanceMapper instanceMapper;
    private final SchedTaskMapper taskMapper;
    private final SchedWorkflowMapper workflowMapper;
    private final TransactionTemplate transactionTemplate;

    public DistributedJobManager(SupervisorProperties conf,
                                 SchedJobMapper jobMapper,
                                 SchedDependMapper dependMapper,
                                 SchedInstanceMapper instanceMapper,
                                 SchedTaskMapper taskMapper,
                                 SchedWorkflowMapper workflowMapper,
                                 IdGenerator idGenerator,
                                 SupervisorRegistry discoveryWorker,
                                 TaskDispatcher taskDispatcher,
                                 GroupedServerInvoker<WorkerRpcService> groupedWorkerRpcClient,
                                 DestinationServerInvoker<WorkerRpcService, Worker> destinationWorkerRpcClient,
                                 @Qualifier(SPRING_BEAN_NAME_TX_TEMPLATE) TransactionTemplate transactionTemplate) {
        super(conf, jobMapper, dependMapper, idGenerator, discoveryWorker, taskDispatcher, groupedWorkerRpcClient, destinationWorkerRpcClient);
        this.instanceMapper = instanceMapper;
        this.taskMapper = taskMapper;
        this.workflowMapper = workflowMapper;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Listen task dispatch failed event
     * <p> {@code `@Async`}需要标注{@code `@EnableAsync`}来启用，默认使用的是`SimpleAsyncTaskExecutor`线程池，会为每个任务创建一个新线程(慎用)
     */
    @EventListener
    public void processTaskDispatchFailedEvent(TaskDispatchFailedEvent event) {
        if (!shouldTerminateDispatchFailedTask(event.getTaskId())) {
            return;
        }
        int toState = ExecuteState.DISPATCH_FAILED.value();
        int fromState = ExecuteState.WAITING.value();
        if (isNotAffectedRow(taskMapper.terminate(event.getTaskId(), null, toState, fromState, null, null))) {
            log.warn("Terminate dispatch failed task unsuccessful: {}", event.getTaskId());
        }
    }

    // ------------------------------------------------------------------database single operation without spring transactional

    public boolean updateInstanceNextScanTime(SchedInstance instance, Date nextScanTime) {
        Assert.notNull(nextScanTime, "Instance next scan time cannot be null.");
        return isOneAffectedRow(instanceMapper.updateNextScanTime(instance.getInstanceId(), nextScanTime, instance.getVersion()));
    }

    public boolean savepoint(long taskId, String worker, String executeSnapshot) {
        return isOneAffectedRow(taskMapper.savepoint(taskId, worker, executeSnapshot));
    }

    @Override
    protected void abortBroadcastWaitingTask(long taskId) {
        taskMapper.terminate(taskId, null, ExecuteState.BROADCAST_ABORTED.value(), ExecuteState.WAITING.value(), null, null);
    }

    @Override
    protected List<SchedTask> listPausableTasks(long instanceId) {
        return taskMapper.findBaseByInstanceId(instanceId, ES_PAUSABLE);
    }

    // ------------------------------------------------------------------database operation within spring @Transactional

    @Transactional(transactionManager = SPRING_BEAN_NAME_TX_MANAGER, rollbackFor = Exception.class)
    public void triggerJob(SchedJob job, RunType runType, long triggerTime) throws JobException {
        Assert.isTrue(runType.isUniqueFlag(), () -> "Job run type must be unique flag mode: " + job);
        TriggerInstanceCreator<TriggerInstance> creator = TriggerInstanceCreator.of(job.getJobType(), this);
        TriggerInstance tInstance = creator.create(job, runType, triggerTime);
        // If SCHEDULE, must be update job next trigger time
        if (runType != RunType.SCHEDULE || hasAffectedRow(jobMapper.updateNextTriggerTime(job))) {
            saveInstanceAndTasks(tInstance);
            TransactionUtils.doAfterTransactionCommit(() -> creator.dispatch(job, tInstance));
        }
    }

    /**
     * Set or clear task worker
     *
     * <pre>
     * 当worker不相同时，可使用`CASE WHEN`语法：
     * UPDATE sched_task SET worker =
     *   CASE
     *     WHEN task_id = 1 THEN 'a'
     *     WHEN task_id = 2 THEN 'b'
     *   END
     * WHERE id IN (1, 2)
     * </pre>
     *
     * @param worker  the worker
     * @param taskIds the task id list
     */
    @Transactional(transactionManager = SPRING_BEAN_NAME_TX_MANAGER, rollbackFor = Exception.class)
    public void updateTaskWorker(String worker, List<Long> taskIds) {
        if (CollectionUtils.isNotEmpty(taskIds)) {
            // Sort for prevent sql deadlock: Deadlock found when trying to get lock; try restarting transaction
            Collections.sort(taskIds);
            Lists.partition(taskIds, PROCESS_BATCH_SIZE).forEach(ids -> taskMapper.batchUpdateWorker(worker, ids));
        }
    }

    // ------------------------------------------------------------------database operation within spring transactionTemplate

    /**
     * Starts the task
     *
     * @param param the start task param
     * @return StartTaskResult, if not null start successfully
     */
    public StartTaskResult startTask(StartTaskParam param) {
        return doInSynchronizedTransaction0(param.getInstanceId(), param.getWnstanceId(), lockedKey -> {
            log.info("Task trace [{}] starting: {}", param.getTaskId(), param.getWorker());
            Date now = new Date();
            if (isNotAffectedRow(taskMapper.start(param.getTaskId(), param.getWorker(), now))) {
                return StartTaskResult.failure("Start task failure.");
            }
            // 1、Mysql：如果先`instanceMapper.get(instanceId)`查一次然后`start`，最后再查的话数据可能会被缓存，返回`runState=10`
            // 2、若先lock住，不会出现以上问题
            if (isNotAffectedRow(instanceMapper.start(param.getInstanceId(), now))) {
                SchedInstance instance = instanceMapper.get(param.getInstanceId());
                if (instance == null || !instance.isRunning()) {
                    throw new IllegalStateException("Start instance failure: " + instance);
                }
            }

            SchedTask task = taskMapper.get(param.getTaskId());
            List<PredecessorInstance> predecessorInstances = null;
            if (param.getJobType() == JobType.WORKFLOW) {
                predecessorInstances = findPredecessorInstances(task.getInstanceId(), param.getWnstanceId());
            }
            return StartTaskResult.success(task, predecessorInstances);
        });
    }

    /**
     * Force change instance state
     *
     * @param instanceId     the instance id, unsupported workflow
     * @param toExecuteState the target execute state
     */
    public void changeInstanceState(long instanceId, ExecuteState toExecuteState) {
        Assert.isTrue(toExecuteState != ExecuteState.EXECUTING, () -> "Force change state invalid target: " + toExecuteState);
        Assert.isNull(instanceMapper.getWnstanceId(instanceId), () -> "Force change state unsupported workflow: " + instanceId);
        doInSynchronizedTransaction(instanceId, null, instance -> {
            RunState fromRunState = RunState.of(instance.getRunState());
            RunState toRunState = toExecuteState.runState();
            Assert.isTrue(fromRunState != RunState.RUNNING, "Force change state current cannot be RUNNING.");
            Assert.isTrue(fromRunState != toRunState, () -> "Force change state current cannot equals target " + toRunState);

            int instRow = instanceMapper.updateState(instanceId, toRunState.value(), fromRunState.value());
            int taskRow = taskMapper.forceChangeState(instanceId, toExecuteState.value());
            if (instRow == 0 && taskRow == 0) {
                throw new IllegalStateException("Force change state failed: " + instanceId);
            }
            if (toExecuteState == ExecuteState.WAITING) {
                Tuple3<SchedJob, SchedInstance, List<SchedTask>> param = buildDispatchParam(instanceId, taskRow);
                TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(param.a, param.b, param.c));
            }
            log.info("Force change state success {}, {}", instanceId, toExecuteState);
        });
    }

    public void deleteInstance(long instanceId) {
        doInSynchronizedTransaction(instanceId, requireWnstanceIdIfWorkflow(instanceId), instance -> {
            Assert.isTrue(instance.isTerminal(), () -> "Deleting instance must be terminal: " + instance);
            if (instance.isWorkflow()) {
                Assert.isTrue(instance.isWorkflowLead(), () -> "Delete instance must be workflow lead: " + instanceId);

                // delete workflow lead instance
                int row = instanceMapper.deleteByInstanceId(instanceId);
                assertOneAffectedRow(row, () -> "Delete workflow lead instance conflict: " + instanceId);

                // delete task
                for (SchedInstance e : instanceMapper.findWorkflowNode(instance.getWnstanceId())) {
                    row = taskMapper.deleteByInstanceId(e.getInstanceId());
                    assertHasAffectedRow(row, () -> "Delete sched task conflict: " + instanceId);
                }

                // delete workflow node instance
                row = instanceMapper.deleteByWnstanceId(instanceId);
                assertHasAffectedRow(row, () -> "Delete workflow node instance conflict: " + instanceId);

                // delete workflow config
                row = workflowMapper.deleteByWnstanceId(instanceId);
                assertHasAffectedRow(row, () -> "Delete sched workflow conflict: " + instanceId);
            } else {
                int row = instanceMapper.deleteByInstanceId(instanceId);
                assertOneAffectedRow(row, () -> "Delete sched instance conflict: " + instanceId);

                row = taskMapper.deleteByInstanceId(instanceId);
                assertHasAffectedRow(row, () -> "Delete sched task conflict: " + instanceId);
            }

            log.info("Delete sched instance success {}", instanceId);
        });
    }

    /**
     * Stop task
     *
     * @param param the stop task param
     * @return {@code true} if stopped task successful
     */
    public boolean stopTask(StopTaskParam param) {
        Assert.hasText(param.getWorker(), "Stop task worker cannot be blank.");
        ExecuteState toState = param.getToState();
        long instanceId = param.getInstanceId();
        long taskId = param.getTaskId();
        Assert.isTrue(toState != ExecuteState.EXECUTING, () -> "Stop task invalid to state " + toState);
        log.info("Task trace [{}] stopping: {}, {}, {}", taskId, param.getOperation(), param.getToState(), param.getWorker());

        return doInSynchronizedTransaction(instanceId, param.getWnstanceId(), instance -> {
            Assert.isTrue(!instance.isWorkflowLead(), () -> "Stop task instance cannot be workflow lead: " + instance);
            if (instance.isTerminal()) {
                // already terminated
                return false;
            }

            int fromState = ExecuteState.EXECUTING.value();
            Date executeEndTime = toState.isTerminal() ? new Date() : null;
            String errMsg = param.getErrorMsg();
            if (isNotAffectedRow(taskMapper.terminate(taskId, param.getWorker(), toState.value(), fromState, executeEndTime, errMsg))) {
                // usual is worker invoke http timeout, then retry
                log.warn("Conflict stop executing task: {}, {}", taskId, toState);
                return false;
            }

            if (toState == ExecuteState.WAITING) {
                Assert.isTrue(param.getOperation() == Operation.SHUTDOWN_RESUME, () -> "Operation must be RESUME: " + param.getOperation());
                if (!updateInstanceNextScanTime(instance, new Date(System.currentTimeMillis() + conf.getShutdownTaskDelayResumeMs()))) {
                    log.warn("Resume task renew instance update time failed: {}", taskId);
                }
                return true;
            }

            Tuple2<RunState, Date> tuple = obtainRunState(taskMapper.findBaseByInstanceId(instanceId, null));
            if (tuple == null) {
                // If the instance has (WAITING or EXECUTING) task
                return true;
            }

            if (!tuple.a.isTerminal()) {
                Assert.isTrue(tuple.a == RunState.PAUSED, () -> "Expect pause run state, but actual: " + tuple.a);
                pauseInstance(instance.isWorkflow() ? instanceMapper.get(instance.getWnstanceId()) : instance);
                return true;
            }

            if (hasAffectedRow(instanceMapper.terminate(instanceId, tuple.a.value(), RS_TERMINABLE, tuple.b))) {
                // the last executing task of this sched instance
                if (param.getOperation().isTrigger()) {
                    instance.markTerminated(tuple.a, tuple.b);
                    afterTerminateTask(instance);
                } else if (instance.isWorkflowNode()) {
                    Assert.isTrue(tuple.a == RunState.CANCELED, () -> "Invalid workflow non-trigger stop state: " + tuple.a);
                    updateWorkflowCurNodeState(instance, tuple.a, RS_TERMINABLE);
                    updateWorkflowFreeNodeState(instanceMapper.get(instance.getWnstanceId()), tuple.a, RS_RUNNABLE);
                }
            }

            return true;
        });
    }

    /**
     * Purge the zombie instance which maybe dead
     *
     * @param inst the sched instance
     * @return {@code true} if purged successfully
     */
    public boolean purgeInstance(SchedInstance inst) {
        Long instanceId = inst.getInstanceId();
        log.info("Purge instance: {}", instanceId);
        return doInSynchronizedTransaction(instanceId, inst.getWnstanceId(), instance -> {
            Assert.isTrue(!instance.isWorkflowLead(), () -> "Purge instance cannot be workflow lead: " + instance);
            // instance run state must in (10, 20)
            if (!RS_PAUSABLE.contains(instance.getRunState())) {
                return false;
            }

            // task execute state must not 10
            List<SchedTask> tasks = taskMapper.findBaseByInstanceId(instanceId, null);
            if (tasks.stream().anyMatch(e -> ExecuteState.WAITING.equalsValue(e.getExecuteState()))) {
                log.warn("Purge instance failed, has waiting task: {}", tasks);
                return false;
            }

            // if task execute state is 20, cannot is alive
            if (hasAliveExecuting(tasks)) {
                log.warn("Purge instance failed, has alive executing task: {}", tasks);
                return false;
            }

            Tuple2<RunState, Date> tuple = obtainRunState(tasks);
            if (tuple == null) {
                tuple = Tuple2.of(RunState.CANCELED, new Date());
            } else {
                // cannot be paused
                Assert.isTrue(tuple.a.isTerminal(), () -> "Purge instance state must be terminal state: " + instance);
            }
            if (isNotAffectedRow(instanceMapper.terminate(instanceId, tuple.a.value(), RS_TERMINABLE, tuple.b))) {
                return false;
            }

            tasks.stream().filter(e -> ES_PAUSABLE.contains(e.getExecuteState())).forEach(e -> {
                String worker = ExecuteState.EXECUTING.equalsValue(e.getExecuteState()) ? Strings.requireNonBlank(e.getWorker()) : null;
                taskMapper.terminate(e.getTaskId(), worker, ExecuteState.EXECUTE_TIMEOUT.value(), e.getExecuteState(), new Date(), null);
            });

            instance.markTerminated(tuple.a, tuple.b);
            afterTerminateTask(instance);

            log.warn("Purge instance {} to state {}", instanceId, tuple.a);
            return true;
        });
    }

    /**
     * Pause instance
     *
     * @param instanceId the instance id, if workflow must be lead instance id
     * @return {@code true} if paused successfully
     */
    public boolean pauseInstance(long instanceId) {
        log.info("Pause instance: {}", instanceId);
        return doInSynchronizedTransaction(instanceId, requireWnstanceIdIfWorkflow(instanceId), instance -> {
            return doIfTrue(RS_PAUSABLE.contains(instance.getRunState()), () -> pauseInstance(instance));
        });
    }

    /**
     * Cancel instance
     *
     * @param instanceId the instance id, if workflow must be lead instance id
     * @param ops        the operation
     * @return {@code true} if canceled successfully
     */
    public boolean cancelInstance(long instanceId, Operation ops) {
        log.info("Cancel instance: {}, {}", instanceId, ops);
        Assert.isTrue(ops.toState().isFailure(), () -> "Cancel instance operation invalid: " + ops);
        return doInSynchronizedTransaction(instanceId, requireWnstanceIdIfWorkflow(instanceId), instance -> {
            return doIfTrue(!instance.isTerminal(), () -> cancelInstance(instance, ops));
        });
    }

    /**
     * Resume the instance from paused to waiting state
     *
     * @param instanceId the instance id, if workflow must be lead instance id
     * @return {@code true} if resumed successfully
     */
    public boolean resumeInstance(long instanceId) {
        log.info("Resume instance: {}", instanceId);
        return doInSynchronizedTransaction(instanceId, requireWnstanceIdIfWorkflow(instanceId), instance -> {
            return doIfTrue(instance.isPaused(), () -> resumeInstance(instance));
        });
    }

    // ------------------------------------------------------------------private methods

    private Long requireWnstanceIdIfWorkflow(long instanceId) {
        Long wnstanceId = instanceMapper.getWnstanceId(instanceId);
        if (wnstanceId != null && instanceId != wnstanceId) {
            throw new IllegalArgumentException("Must be workflow wnstance id: " + wnstanceId + ", " + instanceId);
        }
        return wnstanceId;
    }

    private void doInSynchronizedTransaction(long instanceId, Long wnstanceId, Consumer<SchedInstance> action) {
        doInSynchronizedTransaction(instanceId, wnstanceId, convert(action, true));
    }

    /**
     * 加JVM锁是为了避免单节点内对数据库锁的等待及数据连接超时
     *
     * @param instanceId the instance id
     * @param wnstanceId the workflow instance id
     * @param action     the action
     * @return boolean value of action result
     */
    private boolean doInSynchronizedTransaction(long instanceId, Long wnstanceId, Predicate<SchedInstance> action) {
        return doInSynchronizedTransaction0(instanceId, wnstanceId, lockedKey -> {
            SchedInstance lockedInstance = instanceMapper.lock(lockedKey);
            Assert.notNull(lockedInstance, () -> "Locked instance not found: " + lockedKey);
            SchedInstance instance = (instanceId == lockedKey) ? lockedInstance : instanceMapper.get(instanceId);
            Assert.notNull(instance, () -> "Instance not found: " + instanceId);
            if (!Objects.equals(instance.getWnstanceId(), wnstanceId)) {
                throw new IllegalStateException("Inconsistent workflow instance id: " + wnstanceId + ", " + instance);
            }
            return action.test(instance);
        });
    }

    private <T> T doInSynchronizedTransaction0(long instanceId, Long wnstanceId, LongFunction<T> action) {
        // Long.toString(lockKey).intern()
        Long lockedKey = wnstanceId != null ? wnstanceId : (Long) instanceId;
        synchronized (JobConstants.INSTANCE_LOCK_POOL.intern(lockedKey)) {
            return transactionTemplate.execute(status -> action.apply(lockedKey));
        }
    }

    private boolean shouldTerminateDispatchFailedTask(long taskId) {
        SchedTask task = taskMapper.get(taskId);
        if (!ExecuteState.WAITING.equalsValue(task.getExecuteState())) {
            return false;
        }
        int currentDispatchFailedCount = task.getDispatchFailedCount();
        if (currentDispatchFailedCount >= conf.getTaskDispatchFailedCountThreshold()) {
            return true;
        }
        if (isNotAffectedRow(taskMapper.incrementDispatchFailedCount(taskId, currentDispatchFailedCount))) {
            return false;
        }
        return (currentDispatchFailedCount + 1) == conf.getTaskDispatchFailedCountThreshold();
    }

    private Tuple2<RunState, Date> obtainRunState(List<SchedTask> tasks) {
        List<ExecuteState> states = tasks.stream().map(SchedTask::getExecuteState).map(ExecuteState::of).collect(Collectors.toList());
        if (states.stream().allMatch(ExecuteState::isTerminal)) {
            // executeEndTime is null: canceled task maybe never not started
            return Tuple2.of(
                states.stream().anyMatch(ExecuteState::isFailure) ? RunState.CANCELED : RunState.FINISHED,
                tasks.stream().map(SchedTask::getExecuteEndTime).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElseGet(Date::new)
            );
        }
        // if task has WAITING or EXECUTING state, then return null
        return states.stream().anyMatch(ExecuteState.Const.PAUSABLE_LIST::contains) ? null : Tuple2.of(RunState.PAUSED, null);
    }

    private void saveInstanceAndTasks(TriggerInstance tInstance) {
        instanceMapper.insert(tInstance.getInstance().fillUniqueFlag());

        if (tInstance instanceof GeneralInstanceCreator.GeneralInstance) {
            GeneralInstanceCreator.GeneralInstance creator = (GeneralInstanceCreator.GeneralInstance) tInstance;
            Collects.batchProcess(creator.getTasks(), taskMapper::batchInsert, PROCESS_BATCH_SIZE);
        } else if (tInstance instanceof WorkflowInstanceCreator.WorkflowInstance) {
            WorkflowInstanceCreator.WorkflowInstance creator = (WorkflowInstanceCreator.WorkflowInstance) tInstance;
            Collects.batchProcess(creator.getWorkflows(), workflowMapper::batchInsert, PROCESS_BATCH_SIZE);
            for (Tuple2<SchedInstance, List<SchedTask>> sub : creator.getNodeInstances()) {
                instanceMapper.insert(sub.a.fillUniqueFlag());
                Collects.batchProcess(sub.b, taskMapper::batchInsert, PROCESS_BATCH_SIZE);
            }
        } else {
            throw new UnsupportedOperationException("Unknown instance creator type: " + tInstance.getClass());
        }
    }

    private void pauseInstance(SchedInstance instance) {
        if (instance.isWorkflow()) {
            long instanceId = instance.getInstanceId();
            Assert.isTrue(instance.isWorkflowLead(), () -> "Pause instance must be workflow lead: " + instanceId);
            // pause sched_workflow running node
            instanceMapper.findWorkflowNode(instanceId)
                .stream()
                .filter(e -> RS_PAUSABLE.contains(e.getRunState()))
                .forEach(this::pauseInstance0);
            updateWorkflowFreeNodeState(instance, RunState.PAUSED, RS_WAITING);
        } else {
            pauseInstance0(instance);
        }
    }

    private void pauseInstance0(SchedInstance instance) {
        Assert.isTrue(RS_PAUSABLE.contains(instance.getRunState()), () -> "Invalid pause instance state: " + instance);
        long instanceId = instance.getInstanceId();
        Operation ops = Operation.PAUSE;

        // 1、update task state: (WAITING) -> (PAUSE)
        taskMapper.updateStateByInstanceId(instanceId, ops.toState().value(), ES_WAITING, null);

        // 2、load the alive executing tasks
        List<ExecuteTaskParam> executingTasks = loadExecutingTasks(instance, ops);
        if (executingTasks.isEmpty()) {
            // has non executing task, update sched instance state
            Tuple2<RunState, Date> tuple = obtainRunState(taskMapper.findBaseByInstanceId(instanceId, null));
            // must be paused or terminated
            Assert.notNull(tuple, () -> "Pause instance failed: " + instanceId);
            int row = instanceMapper.terminate(instanceId, tuple.a.value(), RS_PAUSABLE, tuple.b);
            assertOneAffectedRow(row, () -> "Pause instance failed: " + instance + ", " + tuple.a);
            if (instance.isWorkflowNode()) {
                updateWorkflowCurNodeState(instance, tuple.a, RS_PAUSABLE);
            }
        } else {
            // has alive executing tasks: dispatch and pause executing tasks
            TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(executingTasks));
        }
    }

    private void cancelInstance(SchedInstance instance, Operation ops) {
        if (instance.isWorkflow()) {
            long instanceId = instance.getInstanceId();
            Assert.isTrue(instance.isWorkflowLead(), () -> "Cancel instance must be workflow lead: " + instanceId);
            instanceMapper.findWorkflowNode(instanceId)
                .stream()
                .filter(e -> !e.isTerminal())
                .forEach(e -> cancelInstance0(e, ops));
            updateWorkflowFreeNodeState(instance, RunState.CANCELED, RS_RUNNABLE);
        } else {
            cancelInstance0(instance, ops);
        }
    }

    private void cancelInstance0(SchedInstance instance, Operation ops) {
        long instanceId = instance.getInstanceId();
        // 1、update: (WAITING or PAUSED) -> (CANCELED)
        taskMapper.updateStateByInstanceId(instanceId, ops.toState().value(), ES_EXECUTABLE, new Date());

        // 2、load the alive executing tasks
        List<ExecuteTaskParam> executingTasks = loadExecutingTasks(instance, ops);
        if (executingTasks.isEmpty()) {
            // has non executing execute_state
            Tuple2<RunState, Date> tuple = obtainRunState(taskMapper.findBaseByInstanceId(instanceId, null));
            Assert.notNull(tuple, () -> "Cancel instance failed: " + instanceId);
            // if all task paused, should update to canceled state
            if (tuple.a == RunState.PAUSED) {
                tuple = Tuple2.of(RunState.CANCELED, new Date());
            }

            RunState toState = tuple.a;
            int row = instanceMapper.terminate(instanceId, toState.value(), RS_TERMINABLE, tuple.b);
            assertOneAffectedRow(row, () -> "Cancel instance failed: " + instance + ", " + toState);
            if (instance.isWorkflowNode()) {
                updateWorkflowCurNodeState(instance, tuple.a, RS_TERMINABLE);
            }
        } else {
            // dispatch and cancel executing tasks
            TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(executingTasks));
        }
    }

    private void resumeInstance(SchedInstance instance) {
        if (instance.isWorkflow()) {
            long instanceId = instance.getInstanceId();
            Assert.isTrue(instance.isWorkflowLead(), () -> "Resume instance must be workflow lead: " + instanceId);
            // update sched_instance paused lead to running state
            int row = instanceMapper.updateState(instanceId, RunState.RUNNING.value(), RunState.PAUSED.value());
            assertOneAffectedRow(row, () -> "Resume workflow lead instance failed: " + instanceId);
            workflowMapper.resumeWaiting(instanceId);
            for (SchedInstance nodeInstance : instanceMapper.findWorkflowNode(instanceId)) {
                if (nodeInstance.isPaused()) {
                    resumeInstance0(nodeInstance);
                    updateWorkflowCurNodeState(nodeInstance, RunState.RUNNING, RS_PAUSED);
                }
            }
            WorkflowGraph graph = new WorkflowGraph(workflowMapper.findByWnstanceId(instanceId));
            LazyLoader<SchedJob> lazyJob = LazyLoader.of(jobMapper::get, instance.getJobId());
            doChecked(() -> processWorkflowNode(instance, graph, graph.map(), lazyJob));
        } else {
            resumeInstance0(instance);
        }
    }

    private void resumeInstance0(SchedInstance instance) {
        long instanceId = instance.getInstanceId();
        int row = instanceMapper.updateState(instanceId, RunState.WAITING.value(), RunState.PAUSED.value());
        assertOneAffectedRow(row, "Resume sched instance failed.");

        row = taskMapper.updateStateByInstanceId(instanceId, ExecuteState.WAITING.value(), ES_PAUSED, null);
        assertHasAffectedRow(row, "Resume sched task failed.");

        // dispatch task
        Tuple3<SchedJob, SchedInstance, List<SchedTask>> param = buildDispatchParam(instanceId, row);
        TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(param.a, param.b, param.c));
    }

    private void afterTerminateTask(SchedInstance instance) {
        Assert.isTrue(!instance.isWorkflowLead(), () -> "After terminate task cannot be workflow lead: " + instance);
        RunState runState = RunState.of(instance.getRunState());
        LazyLoader<SchedJob> lazyJob = LazyLoader.of(jobMapper::get, instance.getJobId());

        boolean retried = false;
        if (runState == RunState.CANCELED) {
            retried = retryJob(instance, lazyJob);
        } else if (runState == RunState.FINISHED) {
            finishRetryOriginalInstanceState(instance);
            processWorkflowInstance(instance, lazyJob);
            dependJob(instance);
        } else {
            throw new IllegalStateException("Unknown terminate run state " + runState);
        }

        if (!retried) {
            renewFixedDelayNextTriggerTime(instance, lazyJob);
        }
    }

    // ------------------------------------------------------------------workflow methods

    private void updateWorkflowCurNodeState(SchedInstance node, RunState toState, List<Integer> fromStates) {
        Assert.isTrue(node.isWorkflowNode(), () -> "Update workflow cur node state must be node: " + node);
        String curNode = node.parseAttach().getCurNode();
        int row = workflowMapper.update(node.getWnstanceId(), curNode, toState.value(), null, fromStates, node.getInstanceId());
        assertHasAffectedRow(row, () -> "Update workflow state failed: " + node + ", " + toState);
    }

    private void updateWorkflowFreeNodeState(SchedInstance lead, RunState toState, List<Integer> fromStates) {
        Assert.isTrue(lead.isWorkflowLead(), () -> "Update workflow free node state must be lead: " + lead);

        long wnstanceId = lead.getWnstanceId();
        workflowMapper.update(wnstanceId, null, toState.value(), null, fromStates, null);
        updateWorkflowTerminalState(wnstanceId, new WorkflowGraph(workflowMapper.findByWnstanceId(wnstanceId)));
    }

    private void processWorkflowInstance(SchedInstance node, LazyLoader<SchedJob> lazyJob) {
        if (!node.isWorkflowNode()) {
            return;
        }
        // update current node state
        updateWorkflowCurNodeState(node, RunState.FINISHED, RS_TERMINABLE);

        // if terminal all, then update workflow nodes
        long wnstanceId = node.getWnstanceId();
        WorkflowGraph graph = new WorkflowGraph(workflowMapper.findByWnstanceId(wnstanceId));
        if (updateWorkflowTerminalState(wnstanceId, graph)) {
            return;
        }

        // process next workflow node
        Map<DAGEdge, SchedWorkflow> map = graph.successors(node.parseAttach().parseCurrentNode());
        SchedInstance lead = instanceMapper.get(wnstanceId);
        // 使用嵌套事务：当出现异常时，可以回滚`processWorkflowNode`方法内的数据操作
        TransactionUtils.doInNestedTransaction(
            transactionTemplate.getTransactionManager(),
            () -> processWorkflowNode(lead, graph, map, lazyJob),
            t -> {
                log.error("Process workflow node error: " + node, t);
                updateWorkflowFreeNodeState(lead, RunState.CANCELED, RS_RUNNABLE);
            }
        );
    }

    private void processWorkflowNode(SchedInstance lead, WorkflowGraph graph,
                                     Map<DAGEdge, SchedWorkflow> map, LazyLoader<SchedJob> lazyJob) throws JobException {
        Assert.isTrue(lead.isWorkflowLead(), () -> "Process workflow node must be lead: " + lead);
        if (map.isEmpty()) {
            return;
        }
        Set<DAGNode> duplicates = new HashSet<>();
        SchedJob job = lazyJob.orElseThrow(() -> new IllegalStateException("Process workflow node, job not found"));
        for (Map.Entry<DAGEdge, SchedWorkflow> edge : map.entrySet()) {
            processWorkflowNode0(job, lead, graph, duplicates, edge);
        }
    }

    private void processWorkflowNode0(SchedJob job, SchedInstance lead, WorkflowGraph graph,
                                      Set<DAGNode> duplicates, Map.Entry<DAGEdge, SchedWorkflow> edge) throws JobException {
        long wnstanceId = lead.getWnstanceId();
        DAGNode target = edge.getKey().getTarget();
        SchedWorkflow workflow = edge.getValue();
        if (target.isEnd() || !workflow.isWaiting() || !duplicates.add(target)) {
            // 当前节点为结束结点 或 当前节点不为等待状态，则跳过
            return;
        }
        Collection<SchedWorkflow> predecessors = graph.predecessors(target).values();
        if (predecessors.stream().anyMatch(e -> !e.isTerminal())) {
            // 前置节点还未结束，则跳过
            return;
        }
        if (predecessors.stream().anyMatch(SchedWorkflow::isFailure)) {
            RunState state = RunState.CANCELED;
            int row = workflowMapper.update(wnstanceId, workflow.getCurNode(), state.value(), null, RS_TERMINABLE, null);
            assertHasAffectedRow(row, () -> "Update workflow cur node state failed: " + workflow + ", " + state);
            return;
        }

        long nextInstanceId = generateId();
        List<SchedTask> tasks = splitJob(SplitJobParam.from(job, target.getName()), nextInstanceId);
        RunType runType = RunType.of(lead.getRunType());
        SchedWorkflow predecessor = predecessors.stream().max(Comparator.comparing(BaseEntity::getUpdatedAt)).orElse(null);
        SchedInstance nextInstance = SchedInstance.create(nextInstanceId, job.getJobId(), runType, System.currentTimeMillis(), 0);
        nextInstance.setRnstanceId(wnstanceId);
        nextInstance.setPnstanceId(predecessor == null ? null : getRetryOriginalInstanceId(instanceMapper.get(predecessor.getInstanceId())));
        nextInstance.setWnstanceId(wnstanceId);
        nextInstance.setAttach(new InstanceAttach(workflow.getCurNode()).toJson());

        int row = workflowMapper.update(wnstanceId, workflow.getCurNode(), RunState.RUNNING.value(), nextInstanceId, RS_WAITING, null);
        assertHasAffectedRow(row, () -> "Start workflow node failed: " + workflow);

        // save to db
        instanceMapper.insert(nextInstance.fillUniqueFlag());
        Collects.batchProcess(tasks, taskMapper::batchInsert, PROCESS_BATCH_SIZE);
        TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(job, nextInstance, tasks));
    }

    private boolean updateWorkflowTerminalState(long wnstanceId, WorkflowGraph graph) {
        if (graph.anyMatch(e -> e.getKey().getTarget().isEnd() && !e.getValue().isTerminal())) {
            // if end node is not terminal state, then process the end node run state
            Map<DAGEdge, SchedWorkflow> ends = graph.predecessors(DAGNode.END);
            if (ends.values().stream().allMatch(SchedWorkflow::isTerminal)) {
                RunState endState = ends.values().stream().anyMatch(SchedWorkflow::isFailure) ? RunState.CANCELED : RunState.FINISHED;
                int row = workflowMapper.update(wnstanceId, DAGNode.END.toString(), endState.value(), null, RS_TERMINABLE, null);
                assertHasAffectedRow(row, () -> "Update workflow end node failed: " + wnstanceId + ", " + endState);
                ends.forEach((k, v) -> graph.get(k.getTarget(), DAGNode.END).setRunState(endState.value()));
            }
        }
        if (graph.allMatch(e -> e.getValue().isTerminal())) {
            // terminate lead instance
            RunState state = graph.anyMatch(e -> e.getValue().isFailure()) ? RunState.CANCELED : RunState.FINISHED;
            int row = instanceMapper.terminate(wnstanceId, state.value(), RS_TERMINABLE, new Date());
            assertOneAffectedRow(row, () -> "Update workflow terminal state failed: " + wnstanceId + ", " + state);
            SchedInstance lead = instanceMapper.get(wnstanceId);
            dependJob(lead);
            renewFixedDelayNextTriggerTime(lead, LazyLoader.of(jobMapper::get, lead.getJobId()));
            return true;
        }
        if (graph.allMatch(e -> e.getValue().isTerminal() || e.getValue().isPaused())) {
            // At Least one paused and others is terminal
            int row = instanceMapper.updateState(wnstanceId, RunState.PAUSED.value(), RunState.RUNNING.value());
            assertOneAffectedRow(row, () -> "Update workflow pause state failed: " + wnstanceId);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------other methods

    private boolean retryJob(SchedInstance prev, LazyLoader<SchedJob> lazyJob) {
        boolean retried = Boolean.TRUE.equals(doCaught(() -> retryJob0(prev, lazyJob)));
        if (!retried && prev.isWorkflowNode()) {
            // If workflow without retry, then require update workflow graph state
            updateWorkflowCurNodeState(prev, RunState.CANCELED, RS_TERMINABLE);
            updateWorkflowFreeNodeState(instanceMapper.get(prev.getWnstanceId()), RunState.CANCELED, RS_RUNNABLE);
        }
        return retried;
    }

    private boolean retryJob0(SchedInstance prev, LazyLoader<SchedJob> lazyJob) throws JobException {
        SchedJob job = lazyJob.orElseThrow(() -> new IllegalStateException("Retry instance, job not found."));
        int retriedCount = prev.obtainRetriedCount();
        if (!job.retryable(RunState.of(prev.getRunState()), retriedCount)) {
            return false;
        }

        // build retry instance
        long triggerTime = job.computeRetryTriggerTime(++retriedCount);
        long retryInstanceId = generateId();
        SchedInstance retryInstance = SchedInstance.create(retryInstanceId, job.getJobId(), RunType.RETRY, triggerTime, retriedCount);
        retryInstance.setRnstanceId(prev.obtainRnstanceId());
        retryInstance.setPnstanceId(getRetryOriginalInstanceId(prev));
        retryInstance.setWnstanceId(prev.getWnstanceId());
        retryInstance.setAttach(prev.getAttach());

        // build retry tasks
        List<SchedTask> tasks = splitRetryTask(job, prev, retryInstanceId);
        Assert.notEmpty(tasks, "Retry instance, split retry task cannot be empty.");

        // 使用嵌套事务：保证`workflow & instance & tasks`操作的原子性
        Runnable dispatchAction = TransactionUtils.doInNestedTransaction(
            transactionTemplate.getTransactionManager(),
            () -> {
                if (prev.isWorkflowNode()) {
                    // 如果是workflow，则需要更新sched_workflow.instance_id
                    String curNode = prev.parseAttach().getCurNode();
                    int row = workflowMapper.update(prev.getWnstanceId(), curNode, null, retryInstanceId, RS_RUNNING, prev.getInstanceId());
                    assertHasAffectedRow(row, () -> "Retry instance, workflow node update failed.");
                }

                instanceMapper.insert(retryInstance.fillUniqueFlag());
                Collects.batchProcess(tasks, taskMapper::batchInsert, PROCESS_BATCH_SIZE);
                return () -> super.dispatch(job, retryInstance, tasks);
            },
            t -> { throw new IllegalStateException("Retry instance, create retry instance fail: " + prev, t); }
        );
        TransactionUtils.doAfterTransactionCommit(dispatchAction);

        return true;
    }

    private List<SchedTask> splitRetryTask(SchedJob job, SchedInstance prev, long instanceId) throws JobException {
        RetryType retryType = RetryType.of(job.getRetryType());
        if (retryType == RetryType.ALL) {
            // re-split tasks
            return splitJob(SplitJobParam.from(job, prev), instanceId);
        }
        if (retryType == RetryType.FAILED) {
            return taskMapper.findLargeByInstanceId(prev.getInstanceId(), null)
                .stream()
                .filter(e -> ExecuteState.of(e.getExecuteState()).isFailure())
                // Broadcast task must be retried with the same worker
                .filter(e -> RouteStrategy.of(job.getRouteStrategy()).isNotBroadcast() || super.isAliveWorker(e.getWorker()))
                .map(e -> SchedTask.create(e.getTaskParam(), generateId(), instanceId, e.getTaskNo(), e.getTaskCount(), e.getWorker()))
                .collect(Collectors.toList());
        }
        // cannot happen
        throw new IllegalArgumentException("Retry instance, unknown retry type: " + job.getJobId() + ", " + retryType);
    }

    /**
     * Crates dependency job instance and task.
     *
     * @param parent the parent instance
     */
    private void dependJob(SchedInstance parent) {
        if (parent.isWorkflowNode() || !parent.isFinished()) {
            return;
        }
        for (SchedDepend depend : dependMapper.findByParentJobId(parent.getJobId())) {
            doCaught(() -> dependJob0(parent, depend), () -> "Depend job error: " + parent + ", " + depend);
        }
    }

    private void dependJob0(SchedInstance parent, SchedDepend depend) throws JobException {
        SchedJob childJob = jobMapper.get(depend.getChildJobId());
        Assert.notNull(childJob, "Depend child job not found.");
        if (JobState.DISABLED.equalsValue(childJob.getJobState())) {
            log.warn("Depend child job disabled: {}", childJob);
            return;
        }

        TriggerInstanceCreator<TriggerInstance> creator = TriggerInstanceCreator.of(childJob.getJobType(), this);
        TriggerInstance tInstance = creator.create(childJob, RunType.DEPEND, System.currentTimeMillis());
        tInstance.getInstance().setRnstanceId(parent.obtainRnstanceId());
        tInstance.getInstance().setPnstanceId(getRetryOriginalInstanceId(parent));

        // 使用嵌套事务：保证`saveInstanceAndTasks`方法内部数据操作的原子性
        Runnable dispatchAction = TransactionUtils.doInNestedTransaction(
            transactionTemplate.getTransactionManager(),
            () -> {
                saveInstanceAndTasks(tInstance);
                return () -> creator.dispatch(childJob, tInstance);
            },
            t -> log.error("Create depend instance failed: " + childJob + ", " + parent, t)
        );
        TransactionUtils.doAfterTransactionCommit(dispatchAction);
    }

    private List<ExecuteTaskParam> loadExecutingTasks(SchedInstance instance, Operation ops) {
        List<ExecuteTaskParam> executingTasks = new ArrayList<>();
        LazyLoader<ExecuteTaskParam.Builder> lazyBuilder = LazyLoader.of(() -> newExecuteTaskParamBuilder(instance));
        long triggerTime = System.currentTimeMillis();
        for (SchedTask task : taskMapper.findBaseByInstanceId(instance.getInstanceId(), ES_EXECUTING)) {
            Worker worker = Worker.deserialize(task.getWorker());
            if (super.isAliveWorker(worker)) {
                executingTasks.add(lazyBuilder.get().build(ops, task.getTaskId(), triggerTime, worker));
            } else {
                // update dead task
                Date executeEndTime = ops.toState().isTerminal() ? new Date() : null;
                int toState = ExecuteState.EXECUTE_TIMEOUT.value();
                int fromState = ExecuteState.EXECUTING.value();
                if (hasAffectedRow(taskMapper.terminate(task.getTaskId(), task.getWorker(), toState, fromState, executeEndTime, null))) {
                    log.info("Terminate dead worker executing task success: {}", task);
                } else {
                    log.error("Terminate dead worker executing task failed: {}", task);
                }
            }
        }
        return executingTasks;
    }

    private Tuple3<SchedJob, SchedInstance, List<SchedTask>> buildDispatchParam(long instanceId, int expectTaskSize) {
        SchedInstance instance = instanceMapper.get(instanceId);
        SchedJob job = jobMapper.get(instance.getJobId());
        Assert.notNull(job, () -> "Not found job: " + instance.getJobId());
        List<SchedTask> waitingTasks = taskMapper.findLargeByInstanceId(instanceId, ES_WAITING);
        if (waitingTasks.size() != expectTaskSize) {
            throw new IllegalStateException("Invalid dispatch tasks size: " + expectTaskSize + ", " + waitingTasks.size());
        }
        return Tuple3.of(job, instance, waitingTasks);
    }

    private List<PredecessorInstance> findPredecessorInstances(long instanceId, long wnstanceId) {
        List<SchedWorkflow> workflows = workflowMapper.findByWnstanceId(wnstanceId);
        SchedWorkflow curWorkflow = Collects.findAny(workflows, e -> Long.valueOf(instanceId).equals(e.getInstanceId()));
        if (curWorkflow == null || DAGNode.fromString(curWorkflow.getPreNode()).isStart()) {
            return null;
        }
        DAGNode curNode = DAGNode.fromString(curWorkflow.getCurNode());
        WorkflowGraph graph = new WorkflowGraph(workflows);
        return Collects.convert(graph.predecessors(curNode).values(), e -> {
            List<SchedTask> tasks = taskMapper.findLargeByInstanceId(e.getInstanceId(), null);
            tasks.sort(Comparator.comparing(SchedTask::getTaskNo));
            return PredecessorInstance.of(e, tasks);
        });
    }

    private void finishRetryOriginalInstanceState(SchedInstance instance) {
        if (!RunType.RETRY.equalsValue(instance.getRunType())) {
            return;
        }
        long pnstanceId = getRetryOriginalInstanceId(instance);
        boolean updated = isOneAffectedRow(instanceMapper.updateState(pnstanceId, RunState.FINISHED.value(), RunState.CANCELED.value()));
        log.info("Updated retry instance state to finished: {}, {}, {}", pnstanceId, instance.getInstanceId(), updated);
    }

    private long getRetryOriginalInstanceId(SchedInstance instance) {
        if (!RunType.RETRY.equalsValue(instance.getRunType())) {
            return instance.getInstanceId();
        }
        int counter = 1;
        long pnstanceId = instance.getPnstanceId();
        for (Long id = pnstanceId; (id = instanceMapper.getPnstanceId(id, RunType.RETRY.value())) != null; ) {
            if (++counter > conf.getMaximumJobRetryCount()) {
                String format = "Retried instance [%d] exceed maximum retry count value: %d > %d";
                throw new IllegalStateException(String.format(format, instance.getInstanceId(), counter, conf.getMaximumJobRetryCount()));
            }
            pnstanceId = id;
        }
        return pnstanceId;
    }

    private void renewFixedDelayNextTriggerTime(SchedInstance instance, LazyLoader<SchedJob> lazyJob) {
        if (instance.isWorkflowNode()) {
            return;
        }
        // 必须是SCHEDULE方式运行的实例：不会存在`A -> ... -> A`问题，因为在添加Job时做了不能循环依赖的校验
        long rnstanceId = instance.obtainRnstanceId();
        SchedInstance root = (rnstanceId == instance.getInstanceId()) ? instance : instanceMapper.get(rnstanceId);
        if (!root.getJobId().equals(instance.getJobId()) || !RunType.SCHEDULE.equalsValue(root.getRunType())) {
            return;
        }
        SchedJob job = lazyJob.orElse(null);
        TriggerType triggerType;
        if (job == null || (triggerType = TriggerType.of(job.getTriggerType())) != TriggerType.FIXED_DELAY) {
            return;
        }
        long nextTriggerTime = triggerType.computeNextTriggerTime(job.getTriggerValue(), instance.getRunEndTime()).getTime();
        boolean updated = isOneAffectedRow(jobMapper.updateFixedDelayNextTriggerTime(job.getJobId(), nextTriggerTime));
        log.info("Update fixed delay next trigger time: {}, {}, {}", job.getJobId(), nextTriggerTime, updated);
    }

}
