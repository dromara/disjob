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
import cn.ponfee.disjob.supervisor.application.SchedGroupService;
import cn.ponfee.disjob.supervisor.configuration.SupervisorProperties;
import cn.ponfee.disjob.supervisor.dag.WorkflowGraph;
import cn.ponfee.disjob.supervisor.dao.mapper.*;
import cn.ponfee.disjob.supervisor.instance.GeneralInstanceCreator;
import cn.ponfee.disjob.supervisor.instance.TriggerInstance;
import cn.ponfee.disjob.supervisor.instance.TriggerInstanceCreator;
import cn.ponfee.disjob.supervisor.instance.WorkflowInstanceCreator;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
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

    private static final List<Integer> RUN_STATE_TERMINABLE = Collects.convert(RunState.Const.TERMINABLE_LIST, RunState::value);
    private static final List<Integer> RUN_STATE_RUNNABLE = Collects.convert(RunState.Const.RUNNABLE_LIST, RunState::value);
    private static final List<Integer> RUN_STATE_PAUSABLE = Collects.convert(RunState.Const.PAUSABLE_LIST, RunState::value);
    private static final List<Integer> RUN_STATE_WAITING = Collections.singletonList(RunState.WAITING.value());
    private static final List<Integer> RUN_STATE_RUNNING = Collections.singletonList(RunState.RUNNING.value());
    private static final List<Integer> RUN_STATE_PAUSED = Collections.singletonList(RunState.PAUSED.value());

    private static final List<Integer> EXECUTE_STATE_EXECUTABLE = Collects.convert(ExecuteState.Const.EXECUTABLE_LIST, ExecuteState::value);
    private static final List<Integer> EXECUTE_STATE_PAUSABLE = Collects.convert(ExecuteState.Const.PAUSABLE_LIST, ExecuteState::value);
    private static final List<Integer> EXECUTE_STATE_WAITING = Collections.singletonList(ExecuteState.WAITING.value());
    private static final List<Integer> EXECUTE_STATE_PAUSED = Collections.singletonList(ExecuteState.PAUSED.value());

    private final TransactionTemplate transactionTemplate;
    private final SchedInstanceMapper instanceMapper;
    private final SchedTaskMapper taskMapper;
    private final SchedWorkflowMapper workflowMapper;

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
        this.transactionTemplate = transactionTemplate;
        this.instanceMapper = instanceMapper;
        this.taskMapper = taskMapper;
        this.workflowMapper = workflowMapper;
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
        return taskMapper.findBaseByInstanceIdAndStates(instanceId, EXECUTE_STATE_PAUSABLE);
    }

    // ------------------------------------------------------------------database operation within spring @Transactional

    /**
     * Manual trigger the sched job
     *
     * @param jobId the job id
     * @throws JobException if occur error
     */
    @Transactional(transactionManager = SPRING_BEAN_NAME_TX_MANAGER, rollbackFor = Exception.class)
    public void triggerJob(long jobId) throws JobException {
        SchedJob job = jobMapper.get(jobId);
        Assert.notNull(job, () -> "Sched job not found: " + jobId);

        TriggerInstanceCreator creator = TriggerInstanceCreator.of(job.getJobType(), this);
        TriggerInstance tInstance = creator.create(job, RunType.MANUAL, System.currentTimeMillis());
        createInstance(tInstance);
        TransactionUtils.doAfterTransactionCommit(() -> creator.dispatch(job, tInstance));
    }

    /**
     * Update sched job, save sched instance and tasks.
     *
     * @param job             the job
     * @param triggerInstance the trigger instance
     * @return {@code true} if operated success
     */
    @Transactional(transactionManager = SPRING_BEAN_NAME_TX_MANAGER, rollbackFor = Exception.class)
    public boolean createInstance(SchedJob job, TriggerInstance triggerInstance) {
        return doIfTrue(isOneAffectedRow(jobMapper.updateNextTriggerTime(job)), () -> createInstance(triggerInstance));
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

            // start instance: if instanceAffectedRow=0 means started by other task
            int instanceAffectedRow = instanceMapper.start(param.getInstanceId(), now);
            SchedInstance instance = instanceMapper.get(param.getInstanceId());
            if (instance == null) {
                return StartTaskResult.failure("Instance not found");
            }
            if (!RunState.RUNNING.equalsValue(instance.getRunState())) {
                return StartTaskResult.failure("Instance cannot startable: " + RunState.of(instance.getRunState()));
            }

            // start task
            if (isNotAffectedRow(taskMapper.start(param.getTaskId(), param.getWorker(), now))) {
                // if instanceAffectedRow > 0, then throw exception for rollback transaction
                Assert.state(instanceAffectedRow == 0, () -> "Start instance cannot affected: " + param);
                return StartTaskResult.failure("Start task failure.");
            }
            SchedTask task = taskMapper.get(param.getTaskId());
            List<PredecessorInstance> predecessorInstances = null;
            if (param.getJobType() == JobType.WORKFLOW) {
                predecessorInstances = findPredecessorInstances(task.getInstanceId(), instance.getWnstanceId());
            }
            return StartTaskResult.success(instance.getJobId(), instance.getWnstanceId(), task, predecessorInstances);
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
            Assert.isTrue(RunState.of(instance.getRunState()).isTerminal(), () -> "Deleting instance must be terminal: " + instance);
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
        Assert.isTrue(toState != ExecuteState.EXECUTING, () -> "Stop task invalid to state " + toState);
        log.info("Task trace [{}] stopping: {}, {}, {}", param.getTaskId(), param.getOperation(), param.getToState(), param.getWorker());

        return doInSynchronizedTransaction(instanceId, param.getWnstanceId(), instance -> {
            Assert.isTrue(!instance.isWorkflowLead(), () -> "Stop task instance cannot be workflow lead: " + instance);
            if (RunState.of(instance.getRunState()).isTerminal()) {
                // already terminated
                return false;
            }

            Date executeEndTime = toState.isTerminal() ? new Date() : null;
            int fromState = ExecuteState.EXECUTING.value();
            int row = taskMapper.terminate(param.getTaskId(), param.getWorker(), toState.value(), fromState, executeEndTime, param.getErrorMsg());
            if (isNotAffectedRow(row)) {
                // usual is worker invoke http timeout, then retry
                log.warn("Conflict stop executing task: {}, {}", param.getTaskId(), toState);
                return false;
            }

            if (toState == ExecuteState.WAITING) {
                Assert.isTrue(param.getOperation() == Operation.SHUTDOWN_RESUME, () -> "Operation expect RESUME, but actual " + param.getOperation());
                if (!updateInstanceNextScanTime(instance, new Date(System.currentTimeMillis() + conf.getShutdownTaskDelayResumeMs()))) {
                    // cannot happen
                    throw new IllegalStateException("Resume task renew instance update time failed: " + param.getTaskId());
                }
                return true;
            }

            Tuple2<RunState, Date> tuple = obtainRunState(taskMapper.findBaseByInstanceId(instanceId));
            if (tuple == null) {
                // If the instance has (WAITING or EXECUTING) task
                return true;
            }

            if (!tuple.a.isTerminal()) {
                Assert.isTrue(tuple.a == RunState.PAUSED, () -> "Expect pause run state, but actual: " + tuple.a);
                pauseInstance(instance.isWorkflow() ? instanceMapper.get(instance.getWnstanceId()) : instance);
                return true;
            }

            if (hasAffectedRow(instanceMapper.terminate(instanceId, tuple.a.value(), RUN_STATE_TERMINABLE, tuple.b))) {
                // the last executing task of this sched instance
                if (param.getOperation().isTrigger()) {
                    instance.markTerminated(tuple.a, tuple.b);
                    afterTerminateTask(instance);
                } else if (instance.isWorkflowNode()) {
                    Assert.isTrue(tuple.a == RunState.CANCELED, () -> "Workflow non-trigger run state not CANCELED: " + tuple.a);
                    updateWorkflowEdgeState(instance, tuple.a.value(), RUN_STATE_TERMINABLE);
                    workflowMapper.update(instance.getWnstanceId(), null, tuple.a.value(), null, RUN_STATE_RUNNABLE, null);
                    updateWorkflowLeadState(instanceMapper.get(param.getWnstanceId()));
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
            if (!RUN_STATE_PAUSABLE.contains(instance.getRunState())) {
                return false;
            }

            // task execute state must not 10
            List<SchedTask> tasks = taskMapper.findBaseByInstanceId(instanceId);
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
            if (isNotAffectedRow(instanceMapper.terminate(instanceId, tuple.a.value(), RUN_STATE_TERMINABLE, tuple.b))) {
                return false;
            }

            tasks.stream()
                .filter(e -> EXECUTE_STATE_PAUSABLE.contains(e.getExecuteState()))
                .forEach(e -> {
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
            return doIfTrue(RUN_STATE_PAUSABLE.contains(instance.getRunState()), () -> pauseInstance(instance));
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
            return doIfTrue(!RunState.of(instance.getRunState()).isTerminal(), () -> cancelInstance(instance, ops));
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
            return doIfTrue(RunState.PAUSED.equalsValue(instance.getRunState()), () -> resumeInstance(instance));
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
            Assert.notNull(instance, () -> "Instance not found: " + instance);
            if (!Objects.equals(instance.getWnstanceId(), wnstanceId)) {
                throw new IllegalArgumentException("Inconsistent workflow instanceId: expect=" + wnstanceId + ", actual=" + instance);
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

    private void createInstance(TriggerInstance tInstance) {
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
                .filter(e -> RUN_STATE_PAUSABLE.contains(e.getRunState()))
                .forEach(this::pauseInstance0);
            // update sched_workflow waiting node to paused state
            workflowMapper.update(instanceId, null, RunState.PAUSED.value(), null, RUN_STATE_WAITING, null);
            // update sched_workflow running lead
            updateWorkflowLeadState(instance);
        } else {
            pauseInstance0(instance);
        }
    }

    private void pauseInstance0(SchedInstance instance) {
        Assert.isTrue(RUN_STATE_PAUSABLE.contains(instance.getRunState()), () -> "Invalid pause instance state: " + instance);
        long instanceId = instance.getInstanceId();
        Operation ops = Operation.PAUSE;

        // 1、update task state: (WAITING) -> (PAUSE)
        taskMapper.updateStateByInstanceId(instanceId, ops.toState().value(), EXECUTE_STATE_WAITING, null);

        // 2、load the alive executing tasks
        List<ExecuteTaskParam> executingTasks = loadExecutingTasks(instance, ops);
        if (executingTasks.isEmpty()) {
            // has non executing task, update sched instance state
            Tuple2<RunState, Date> tuple = obtainRunState(taskMapper.findBaseByInstanceId(instanceId));
            // must be paused or terminated
            Assert.notNull(tuple, () -> "Pause instance failed: " + instanceId);
            int row = instanceMapper.terminate(instanceId, tuple.a.value(), RUN_STATE_PAUSABLE, tuple.b);
            assertOneAffectedRow(row, () -> "Pause instance failed: " + instance + ", " + tuple.a);
            if (instance.isWorkflowNode()) {
                updateWorkflowEdgeState(instance, tuple.a.value(), RUN_STATE_PAUSABLE);
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
                .filter(e -> !RunState.of(e.getRunState()).isTerminal())
                .forEach(e -> cancelInstance0(e, ops));
            workflowMapper.update(instanceId, null, RunState.CANCELED.value(), null, RUN_STATE_RUNNABLE, null);
            updateWorkflowLeadState(instance);
        } else {
            cancelInstance0(instance, ops);
        }
    }

    private void cancelInstance0(SchedInstance instance, Operation ops) {
        long instanceId = instance.getInstanceId();
        // 1、update: (WAITING or PAUSED) -> (CANCELED)
        taskMapper.updateStateByInstanceId(instanceId, ops.toState().value(), EXECUTE_STATE_EXECUTABLE, new Date());

        // 2、load the alive executing tasks
        List<ExecuteTaskParam> executingTasks = loadExecutingTasks(instance, ops);
        if (executingTasks.isEmpty()) {
            // has non executing execute_state
            Tuple2<RunState, Date> tuple = obtainRunState(taskMapper.findBaseByInstanceId(instanceId));
            Assert.notNull(tuple, () -> "Cancel instance failed: " + instanceId);
            // if all task paused, should update to canceled state
            if (tuple.a == RunState.PAUSED) {
                tuple = Tuple2.of(RunState.CANCELED, new Date());
            }

            RunState toState = tuple.a;
            int row = instanceMapper.terminate(instanceId, toState.value(), RUN_STATE_TERMINABLE, tuple.b);
            assertOneAffectedRow(row, () -> "Cancel instance failed: " + instance + ", " + toState);
            if (instance.isWorkflowNode()) {
                updateWorkflowEdgeState(instance, tuple.a.value(), RUN_STATE_TERMINABLE);
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
                if (RunState.PAUSED.equalsValue(nodeInstance.getRunState())) {
                    resumeInstance0(nodeInstance);
                    updateWorkflowEdgeState(nodeInstance, RunState.RUNNING.value(), RUN_STATE_PAUSED);
                }
            }
            WorkflowGraph graph = new WorkflowGraph(workflowMapper.findByWnstanceId(instanceId));
            createWorkflowNode(instance, graph, graph.map(), ExceptionUtils::rethrow);
        } else {
            resumeInstance0(instance);
        }
    }

    private void resumeInstance0(SchedInstance instance) {
        long instanceId = instance.getInstanceId();
        int row = instanceMapper.updateState(instanceId, RunState.WAITING.value(), RunState.PAUSED.value());
        assertOneAffectedRow(row, "Resume sched instance failed.");

        row = taskMapper.updateStateByInstanceId(instanceId, ExecuteState.WAITING.value(), EXECUTE_STATE_PAUSED, null);
        assertHasAffectedRow(row, "Resume sched task failed.");

        // dispatch task
        Tuple3<SchedJob, SchedInstance, List<SchedTask>> param = buildDispatchParam(instanceId, row);
        TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(param.a, param.b, param.c));
    }

    private void updateWorkflowLeadState(SchedInstance instance) {
        Assert.isTrue(instance.isWorkflowLead(), () -> "Update state instance must be workflow lead: " + instance);
        long wnstanceId = instance.getWnstanceId();
        List<SchedWorkflow> workflows = workflowMapper.findByWnstanceId(wnstanceId);

        WorkflowGraph graph = new WorkflowGraph(workflows);
        updateWorkflowEndState(graph);

        if (graph.allMatch(e -> e.getValue().isTerminal())) {
            RunState state = graph.anyMatch(e -> e.getValue().isFailure()) ? RunState.CANCELED : RunState.FINISHED;
            int row = instanceMapper.terminate(instance.getWnstanceId(), state.value(), RUN_STATE_TERMINABLE, new Date());
            assertOneAffectedRow(row, () -> "Update workflow lead instance state failed: " + instance + ", " + state);
        } else if (workflows.stream().noneMatch(e -> RunState.RUNNING.equalsValue(e.getRunState()))) {
            RunState state = RunState.PAUSED;
            int row = instanceMapper.updateState(instance.getWnstanceId(), state.value(), instance.getRunState());
            assertOneAffectedRow(row, () -> "Update workflow lead instance state failed: " + instance + ", " + state);
        }
    }

    private void updateWorkflowEdgeState(SchedInstance instance, Integer toState, List<Integer> fromStates) {
        String curNode = instance.parseAttach().getCurNode();
        int row = workflowMapper.update(instance.getWnstanceId(), curNode, toState, null, fromStates, instance.getInstanceId());
        Assert.isTrue(row > 0, () -> "Update workflow state failed: " + instance + ", " + toState);
    }

    private void updateWorkflowEndState(WorkflowGraph graph) {
        long wnstanceId = Collects.getFirst(graph.map().values()).getWnstanceId();
        // if end node is not terminal state, then process the end node run state
        if (graph.anyMatch(e -> e.getKey().getTarget().isEnd() && !e.getValue().isTerminal())) {
            Map<DAGEdge, SchedWorkflow> ends = graph.predecessors(DAGNode.END);
            if (ends.values().stream().allMatch(SchedWorkflow::isTerminal)) {
                RunState endState = ends.values().stream().anyMatch(SchedWorkflow::isFailure) ? RunState.CANCELED : RunState.FINISHED;
                int row = workflowMapper.update(wnstanceId, DAGNode.END.toString(), endState.value(), null, RUN_STATE_TERMINABLE, null);
                Assert.isTrue(row > 0, () -> "Update workflow end node failed: " + wnstanceId + ", " + endState);
                ends.forEach((k, v) -> graph.get(k.getTarget(), DAGNode.END).setRunState(endState.value()));
            }
        }
    }

    private void createWorkflowNode(SchedInstance leadInstance, WorkflowGraph graph,
                                    Map<DAGEdge, SchedWorkflow> map, Predicate<Throwable> failHandler) {
        SchedJob job = LazyLoader.of(SchedJob.class, jobMapper::get, leadInstance.getJobId());
        long wnstanceId = leadInstance.getWnstanceId();
        Set<DAGNode> duplicates = new HashSet<>();
        for (Map.Entry<DAGEdge, SchedWorkflow> entry : map.entrySet()) {
            DAGNode target = entry.getKey().getTarget();
            SchedWorkflow workflow = entry.getValue();
            if (target.isEnd() || !RunState.WAITING.equalsValue(workflow.getRunState()) || !duplicates.add(target)) {
                // 当前节点为结束结点 或 当前节点不为等待状态，则跳过
                continue;
            }

            Collection<SchedWorkflow> predecessors = graph.predecessors(target).values();
            if (predecessors.stream().anyMatch(e -> !RunState.of(e.getRunState()).isTerminal())) {
                // 前置节点还未结束，则跳过
                continue;
            }

            if (predecessors.stream().anyMatch(e -> RunState.of(e.getRunState()).isFailure())) {
                RunState state = RunState.CANCELED;
                int row = workflowMapper.update(wnstanceId, workflow.getCurNode(), state.value(), null, RUN_STATE_TERMINABLE, null);
                Assert.isTrue(row > 0, () -> "Update workflow cur node state failed: " + workflow + ", " + state);
                continue;
            }

            try {
                long nextInstanceId = generateId();
                List<SchedTask> tasks = splitJob(SplitJobParam.from(job, target.getName()), nextInstanceId);
                RunType runType = RunType.of(leadInstance.getRunType());
                SchedWorkflow predecessor = predecessors.stream().max(Comparator.comparing(BaseEntity::getUpdatedAt)).orElse(null);
                SchedInstance nextInstance = SchedInstance.create(nextInstanceId, job.getJobId(), runType, System.currentTimeMillis(), 0);
                nextInstance.setRnstanceId(wnstanceId);
                nextInstance.setPnstanceId(predecessor == null ? null : getRetryOriginalInstanceId(instanceMapper.get(predecessor.getInstanceId())));
                nextInstance.setWnstanceId(wnstanceId);
                nextInstance.setAttach(new InstanceAttach(workflow.getCurNode()).toJson());

                int row = workflowMapper.update(wnstanceId, workflow.getCurNode(), RunState.RUNNING.value(), nextInstanceId, RUN_STATE_WAITING, null);
                Assert.isTrue(row > 0, () -> "Start workflow node failed: " + workflow);

                // save to db
                instanceMapper.insert(nextInstance.fillUniqueFlag());
                Collects.batchProcess(tasks, taskMapper::batchInsert, PROCESS_BATCH_SIZE);
                TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(job, nextInstance, tasks));
            } catch (Throwable t) {
                if (!failHandler.test(t)) {
                    // if false then break
                    return;
                }
            }
        }
    }

    private void afterTerminateTask(SchedInstance instance) {
        RunState runState = RunState.of(instance.getRunState());
        LazyLoader<SchedJob> lazyJob = LazyLoader.of(jobMapper::get, instance.getJobId());

        if (runState == RunState.CANCELED) {
            retryJob(instance, lazyJob);
        } else if (runState == RunState.FINISHED) {
            finishRetryOriginalInstanceState(instance);
            processWorkflow(instance);
            dependJob(instance);
        } else {
            throw new IllegalStateException("Unknown terminate run state " + runState);
        }

        updateFixedDelayNextTriggerTime(instance, lazyJob);
    }

    private void updateFixedDelayNextTriggerTime(SchedInstance curr, LazyLoader<SchedJob> lazyJob) {
        // 1、不能为工作流任务从节点实例
        if (curr.isWorkflowNode()) {
            return;
        }

        // 2、必须是SCHEDULE方式运行的实例：不会存在`A -> ... -> A`问题，因为在添加Job时做了不能循环依赖的校验
        long rnstanceId = curr.obtainRnstanceId();
        SchedInstance root = (rnstanceId == curr.getInstanceId()) ? curr : instanceMapper.get(rnstanceId);
        if (!root.getJobId().equals(curr.getJobId()) || !RunType.SCHEDULE.equalsValue(root.getRunType())) {
            return;
        }

        // 3、如果是可重试，则要等到最后的那次重试完时来计算下次的延时执行时间
        SchedJob job = lazyJob.orElse(null);
        if (job == null || job.retryable(RunState.of(curr.getRunState()), curr.obtainRetriedCount())) {
            return;
        }

        // 4、do update nextTriggerTime
        super.updateFixedDelayNextTriggerTime(job, curr.getRunEndTime());
    }

    private void processWorkflow(SchedInstance nodeInstance) {
        if (!nodeInstance.isWorkflowNode()) {
            return;
        }

        RunState runState = RunState.of(nodeInstance.getRunState());
        Long wnstanceId = nodeInstance.getWnstanceId();

        updateWorkflowEdgeState(nodeInstance, runState.value(), RUN_STATE_TERMINABLE);

        if (runState == RunState.CANCELED) {
            workflowMapper.update(wnstanceId, null, RunState.CANCELED.value(), null, RUN_STATE_RUNNABLE, null);
        }

        WorkflowGraph graph = new WorkflowGraph(workflowMapper.findByWnstanceId(wnstanceId));
        updateWorkflowEndState(graph);

        // process workflows run state
        if (graph.allMatch(e -> e.getValue().isTerminal())) {
            RunState state = graph.anyMatch(e -> e.getValue().isFailure()) ? RunState.CANCELED : RunState.FINISHED;
            int row = instanceMapper.terminate(wnstanceId, state.value(), RUN_STATE_TERMINABLE, new Date());
            assertOneAffectedRow(row, () -> "Terminate workflow lead instance failed: " + nodeInstance + ", " + state);
            afterTerminateTask(instanceMapper.get(wnstanceId));
            return;
        }

        if (runState == RunState.CANCELED) {
            return;
        }

        Map<DAGEdge, SchedWorkflow> map = graph.successors(nodeInstance.parseAttach().parseCurrentNode());
        createWorkflowNode(instanceMapper.get(wnstanceId), graph, map, throwable -> {
            log.error("Split workflow job task error: " + nodeInstance, throwable);
            onCreateWorkflowNodeFailed(nodeInstance.getWnstanceId());
            return false;
        });
    }

    private void onCreateWorkflowNodeFailed(Long wnstanceId) {
        int canceled = RunState.CANCELED.value();
        workflowMapper.update(wnstanceId, null, canceled, null, RUN_STATE_RUNNABLE, null);
        WorkflowGraph graph = new WorkflowGraph(workflowMapper.findByWnstanceId(wnstanceId));
        updateWorkflowEndState(graph);
        Assert.state(graph.allMatch(e -> e.getValue().isTerminal()), "Workflow not all terminal.");
        int row = instanceMapper.terminate(wnstanceId, canceled, RUN_STATE_TERMINABLE, new Date());
        assertOneAffectedRow(row, () -> "Cancel workflow failed: " + wnstanceId);
        afterTerminateTask(instanceMapper.get(wnstanceId));
    }

    private void retryJob(SchedInstance prev, LazyLoader<SchedJob> lazyJob) {
        if (prev.isWorkflowLead()) {
            return;
        }

        SchedJob schedJob = lazyJob.orElseGet(() -> {
            log.error("Sched job not found: {}", prev.getJobId());
            return null;
        });
        int retriedCount = prev.obtainRetriedCount();
        if (schedJob == null || !schedJob.retryable(RunState.of(prev.getRunState()), retriedCount)) {
            processWorkflow(prev);
            return;
        }

        // 如果是workflow，则需要更新sched_workflow.instance_id
        long retryInstanceId = generateId();
        if (prev.isWorkflowNode()) {
            String curNode = prev.parseAttach().getCurNode();
            int row = workflowMapper.update(prev.getWnstanceId(), curNode, null, retryInstanceId, RUN_STATE_RUNNING, prev.getInstanceId());
            Assert.isTrue(row > 0, () -> "Retry workflow node failed: " + prev);
        }

        // 1、build sched instance
        retriedCount++;
        long triggerTime = schedJob.computeRetryTriggerTime(retriedCount);
        SchedInstance retryInstance = SchedInstance.create(retryInstanceId, schedJob.getJobId(), RunType.RETRY, triggerTime, retriedCount);
        retryInstance.setRnstanceId(prev.obtainRnstanceId());
        retryInstance.setPnstanceId(getRetryOriginalInstanceId(prev));
        retryInstance.setWnstanceId(prev.getWnstanceId());
        retryInstance.setAttach(prev.getAttach());

        // 2、build sched tasks
        List<SchedTask> tasks;
        RetryType retryType = RetryType.of(schedJob.getRetryType());
        if (retryType == RetryType.ALL) {
            try {
                // re-split tasks
                tasks = splitJob(SplitJobParam.from(schedJob, prev), retryInstance.getInstanceId());
            } catch (Throwable t) {
                log.error("Split retry job error: " + schedJob + ", " + prev, t);
                processWorkflow(prev);
                return;
            }
        } else if (retryType == RetryType.FAILED) {
            tasks = taskMapper.findLargeByInstanceId(prev.getInstanceId())
                .stream()
                .filter(e -> ExecuteState.of(e.getExecuteState()).isFailure())
                // broadcast task cannot support partial retry
                .filter(e -> RouteStrategy.of(schedJob.getRouteStrategy()).isNotBroadcast() || super.isAliveWorker(e.getWorker()))
                .map(e -> SchedTask.create(e.getTaskParam(), generateId(), retryInstanceId, e.getTaskNo(), e.getTaskCount(), e.getWorker()))
                .collect(Collectors.toList());
        } else {
            // cannot happen
            throw new IllegalArgumentException("Unknown job retry type: " + schedJob.getJobId() + ", " + retryType);
        }

        // 3、save to db
        Assert.notEmpty(tasks, "Insert list of task cannot be empty.");
        instanceMapper.insert(retryInstance.fillUniqueFlag());
        Collects.batchProcess(tasks, taskMapper::batchInsert, PROCESS_BATCH_SIZE);

        TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(schedJob, retryInstance, tasks));
    }

    /**
     * Crates dependency job task.
     *
     * @param parentInstance the parent instance
     */
    private void dependJob(SchedInstance parentInstance) {
        if (parentInstance.isWorkflowNode()) {
            return;
        }
        List<SchedDepend> schedDepends = dependMapper.findByParentJobId(parentInstance.getJobId());
        if (CollectionUtils.isEmpty(schedDepends)) {
            return;
        }

        for (SchedDepend depend : schedDepends) {
            SchedJob childJob = jobMapper.get(depend.getChildJobId());
            if (childJob == null) {
                log.error("Child sched job not found: {}, {}", depend.getParentJobId(), depend.getChildJobId());
                continue;
            }
            if (JobState.DISABLE.equalsValue(childJob.getJobState())) {
                continue;
            }

            // 嵌套事务PROPAGATION_NESTED：内部事务中如果创建重试任务实例异常(被cache住了并rollback)，并不影响外部事务中的任务实例状态更新
            Runnable dispatchAction = TransactionUtils.doInNestedTransaction(
                Objects.requireNonNull(transactionTemplate.getTransactionManager()),
                () -> {
                    TriggerInstanceCreator creator = TriggerInstanceCreator.of(childJob.getJobType(), this);
                    TriggerInstance tInstance = creator.create(childJob, RunType.DEPEND, System.currentTimeMillis());
                    tInstance.getInstance().setRnstanceId(parentInstance.obtainRnstanceId());
                    tInstance.getInstance().setPnstanceId(getRetryOriginalInstanceId(parentInstance));
                    createInstance(tInstance);
                    return () -> creator.dispatch(childJob, tInstance);
                },
                t -> log.error("Depend job instance created fail: " + parentInstance + ", " + childJob, t)
            );

            if (dispatchAction != null) {
                TransactionUtils.doAfterTransactionCommit(dispatchAction);
            }
        }
    }

    private List<ExecuteTaskParam> loadExecutingTasks(SchedInstance instance, Operation ops) {
        List<ExecuteTaskParam> executingTasks = new ArrayList<>();
        SchedJob schedJob = LazyLoader.of(SchedJob.class, jobMapper::get, instance.getJobId());
        String supervisorToken = SchedGroupService.createSupervisorAuthenticationToken(schedJob.getGroup());
        ExecuteTaskParam.Builder builder = ExecuteTaskParam.builder(instance, schedJob, supervisorToken);
        // immediate trigger
        long triggerTime = 0L;
        for (SchedTask task : taskMapper.findBaseByInstanceId(instance.getInstanceId())) {
            if (!ExecuteState.EXECUTING.equalsValue(task.getExecuteState())) {
                continue;
            }
            Worker worker = Worker.deserialize(task.getWorker());
            if (super.isAliveWorker(worker)) {
                executingTasks.add(builder.build(ops, task.getTaskId(), triggerTime, worker));
            } else {
                // update dead task
                Date executeEndTime = ops.toState().isTerminal() ? new Date() : null;
                int toState = ExecuteState.EXECUTE_TIMEOUT.value();
                int fromState = ExecuteState.EXECUTING.value();
                int row = taskMapper.terminate(task.getTaskId(), task.getWorker(), toState, fromState, executeEndTime, null);
                if (isOneAffectedRow(row)) {
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
        List<SchedTask> waitingTasks = taskMapper.findLargeByInstanceId(instanceId)
            .stream()
            .filter(e -> ExecuteState.WAITING.equalsValue(e.getExecuteState()))
            .collect(Collectors.toList());
        if (waitingTasks.size() != expectTaskSize) {
            throw new IllegalStateException("Invalid dispatching tasks size: expect=" + expectTaskSize + ", actual=" + waitingTasks.size());
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
        WorkflowGraph workflowGraph = new WorkflowGraph(workflows);
        Map<DAGEdge, SchedWorkflow> predecessors = workflowGraph.predecessors(curNode);
        if (MapUtils.isEmpty(predecessors)) {
            return null;
        }
        return predecessors.values()
            .stream()
            .map(e -> {
                List<SchedTask> tasks = taskMapper.findLargeByInstanceId(e.getInstanceId());
                tasks.sort(Comparator.comparing(SchedTask::getTaskNo));
                return PredecessorInstance.of(e, tasks);
            })
            .collect(Collectors.toList());
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

}
