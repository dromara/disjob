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

import cn.ponfee.disjob.alert.Alerter;
import cn.ponfee.disjob.alert.event.AlertInstanceEvent;
import cn.ponfee.disjob.common.base.IdGenerator;
import cn.ponfee.disjob.common.base.Symbol.Str;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.dag.DAGEdge;
import cn.ponfee.disjob.common.dag.DAGNode;
import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingSupplier;
import cn.ponfee.disjob.common.model.BaseEntity;
import cn.ponfee.disjob.common.tuple.Tuple2;
import cn.ponfee.disjob.common.tuple.Tuple3;
import cn.ponfee.disjob.common.util.Strings;
import cn.ponfee.disjob.core.base.CoreUtils;
import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.dag.PredecessorInstance;
import cn.ponfee.disjob.core.dto.supervisor.StartTaskParam;
import cn.ponfee.disjob.core.dto.supervisor.StartTaskResult;
import cn.ponfee.disjob.core.dto.supervisor.StopTaskParam;
import cn.ponfee.disjob.core.dto.worker.SplitJobParam;
import cn.ponfee.disjob.core.enums.*;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import cn.ponfee.disjob.dispatch.event.TaskDispatchFailedEvent;
import cn.ponfee.disjob.supervisor.base.ExecuteTaskParamBuilder;
import cn.ponfee.disjob.supervisor.base.ModelConverter;
import cn.ponfee.disjob.supervisor.base.TriggerTimes;
import cn.ponfee.disjob.supervisor.configuration.SupervisorProperties;
import cn.ponfee.disjob.supervisor.dag.WorkflowGraph;
import cn.ponfee.disjob.supervisor.dao.mapper.*;
import cn.ponfee.disjob.supervisor.exception.KeyExistsException;
import cn.ponfee.disjob.supervisor.instance.TriggerInstance;
import cn.ponfee.disjob.supervisor.model.*;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.lang.Nullable;
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
import static com.google.common.collect.ImmutableList.of;

/**
 * Job manager
 *
 * @author Ponfee
 */
@Component
public class JobManager {

    private static final Logger LOG = LoggerFactory.getLogger(JobManager.class);
    private static final Comparator<Tuple2<Worker, Long>> WORKLOAD_COMPARATOR = Comparator.comparingLong(e -> e.b);
    private static final List<Integer> RS_TERMINABLE = of(RunState.WAITING.value(), RunState.RUNNING.value(), RunState.PAUSED.value());
    private static final List<Integer> RS_RUNNABLE   = of(RunState.WAITING.value(), RunState.PAUSED.value());
    private static final List<Integer> RS_PAUSABLE   = of(RunState.WAITING.value(), RunState.RUNNING.value());
    private static final List<Integer> RS_WAITING    = of(RunState.WAITING.value());
    private static final List<Integer> RS_RUNNING    = of(RunState.RUNNING.value());
    private static final List<Integer> RS_PAUSED     = of(RunState.PAUSED.value());
    private static final List<Integer> ES_EXECUTABLE = of(ExecuteState.WAITING.value(), ExecuteState.PAUSED.value());
    private static final List<Integer> ES_PAUSABLE   = of(ExecuteState.WAITING.value(), ExecuteState.EXECUTING.value());
    private static final List<Integer> ES_WAITING    = of(ExecuteState.WAITING.value());
    private static final List<Integer> ES_EXECUTING  = of(ExecuteState.EXECUTING.value());
    private static final List<Integer> ES_PAUSED     = of(ExecuteState.PAUSED.value());
    private static final List<Integer> ES_COMPLETED  = of(ExecuteState.COMPLETED.value());

    private final SupervisorProperties conf;
    private final IdGenerator idGenerator;
    private final SchedJobMapper jobMapper;
    private final SchedDependMapper dependMapper;
    private final SchedInstanceMapper instanceMapper;
    private final SchedWorkflowMapper workflowMapper;
    private final SchedTaskMapper taskMapper;
    private final WorkerClient workerClient;
    private final Alerter alerter;
    private final TransactionTemplate transactionTemplate;

    public JobManager(SupervisorProperties conf,
                      IdGenerator idGenerator,
                      SchedJobMapper jobMapper,
                      SchedDependMapper dependMapper,
                      SchedInstanceMapper instanceMapper,
                      SchedWorkflowMapper workflowMapper,
                      SchedTaskMapper taskMapper,
                      WorkerClient workerClient,
                      @Nullable Alerter alerter,
                      @Qualifier(SPRING_BEAN_NAME_TX_TEMPLATE) TransactionTemplate txTemplate) {
        conf.check();
        this.conf = conf;
        this.idGenerator = idGenerator;
        this.jobMapper = jobMapper;
        this.dependMapper = dependMapper;
        this.instanceMapper = instanceMapper;
        this.workflowMapper = workflowMapper;
        this.taskMapper = taskMapper;
        this.workerClient = workerClient;
        this.alerter = alerter;
        this.transactionTemplate = txTemplate;
    }

    // ------------------------------------------------------------------non-database operation methods

    public long generateId() {
        return idGenerator.generateId();
    }

    public List<SchedTask> splitJob(String group, long instanceId, SplitJobParam param) throws JobException {
        return workerClient.splitJob(group, instanceId, param, this::generateId, conf.getMaximumSplitTaskSize());
    }

    public boolean dispatch(SchedJob job, SchedInstance instance, List<SchedTask> tasks) {
        return dispatch(false, job, instance, tasks);
    }

    public boolean redispatch(SchedJob job, SchedInstance instance, List<SchedTask> tasks) {
        return dispatch(true, job, instance, tasks);
    }

    // ------------------------------------------------------------------database single operation without spring transactional

    public void disableJob(SchedJob job) {
        jobMapper.disable(job);
    }

    public boolean updateJobNextTriggerTime(SchedJob job) {
        return isOneAffectedRow(jobMapper.updateNextTriggerTime(job));
    }

    public void updateJobNextScanTime(SchedJob job) {
        jobMapper.updateNextScanTime(job);
    }

    public boolean updateInstanceNextScanTime(SchedInstance inst, Date nextScanTime) {
        Assert.notNull(nextScanTime, "Instance next scan time cannot be null.");
        return isOneAffectedRow(instanceMapper.updateNextScanTime(inst.getInstanceId(), nextScanTime, inst.getVersion()));
    }

    public boolean savepoint(long taskId, String worker, String executeSnapshot) {
        CoreUtils.checkClobMaximumLength(executeSnapshot, "Execute snapshot");
        return isOneAffectedRow(taskMapper.savepoint(taskId, worker, executeSnapshot));
    }

    // ------------------------------------------------------------------must in transaction active(propagation Mandatory)

    public void saveLeadInstanceAndWorkflows(SchedInstance instance, List<SchedWorkflow> workflows) {
        Assert.isTrue(instance.isWorkflowLead(), () -> "Must be workflow lead instance: " + instance);
        assertWithinTransaction();
        instanceMapper.insert(instance.fillUniqueFlag());
        Collects.batchProcess(workflows, workflowMapper::batchInsert, PROCESS_BATCH_SIZE);
    }

    public void saveInstanceAndTasks(SchedInstance instance, List<SchedTask> tasks) {
        assertWithinTransaction();
        instanceMapper.insert(instance.fillUniqueFlag());
        Collects.batchProcess(tasks, taskMapper::batchInsert, PROCESS_BATCH_SIZE);
    }

    // ------------------------------------------------------------------database operation within spring @Transactional

    @Transactional(transactionManager = SPRING_BEAN_NAME_TX_MANAGER, rollbackFor = Exception.class)
    public Long addJob(SchedJob job) throws JobException {
        job.verifyForAdd(conf.getMaximumJobRetryCount());
        if (jobMapper.getJobId(job.getGroup(), job.getJobName()) != null) {
            throw new KeyExistsException("Exists job name: " + job.getJobName());
        }
        workerClient.verifyJob(job);
        job.setJobId(generateId());
        parseTriggerConfig(job);

        jobMapper.insert(job);
        return job.getJobId();
    }

    @Transactional(transactionManager = SPRING_BEAN_NAME_TX_MANAGER, rollbackFor = Exception.class)
    public void updateJob(SchedJob job) throws JobException {
        job.verifyForUpdate(conf.getMaximumJobRetryCount());
        if (job.isNeedUpdateExecutor()) {
            workerClient.verifyJob(job);
        }
        Long jobId0 = jobMapper.getJobId(job.getGroup(), job.getJobName());
        if (jobId0 != null && !jobId0.equals(job.getJobId())) {
            throw new IllegalArgumentException("Exists job name: " + job.getJobName());
        }

        SchedJob dbJob = jobMapper.get(job.getJobId());
        Assert.notNull(dbJob, () -> "Sched job id not found " + job.getJobId());
        Assert.isTrue(dbJob.getGroup().equals(job.getGroup()), "Job group cannot be modify.");
        if (job.isNeedUpdateTrigger(dbJob.getTriggerType(), dbJob.getTriggerValue())) {
            dependMapper.deleteByChildJobId(job.getJobId());
            parseTriggerConfig(job);
        }

        assertOneAffectedRow(jobMapper.update(job), "Update sched job fail or conflict.");
    }

    @Transactional(transactionManager = SPRING_BEAN_NAME_TX_MANAGER, rollbackFor = Exception.class)
    public void deleteJob(String user, long jobId) {
        SchedJob job = jobMapper.get(jobId);
        Assert.notNull(job, () -> "Job id not found: " + jobId);
        Assert.state(!job.isEnabled(), "Please disable job before delete this job.");
        assertOneAffectedRow(jobMapper.softDelete(jobId, user), "Delete sched job fail or conflict.");
        dependMapper.deleteByParentJobId(jobId);
        dependMapper.deleteByChildJobId(jobId);
    }

    @Transactional(transactionManager = SPRING_BEAN_NAME_TX_MANAGER, rollbackFor = Exception.class)
    public void changeJobState(String user, long jobId, JobState toState) {
        if (isNotAffectedRow(jobMapper.updateState(jobId, user, toState.value(), 1 ^ toState.value()))) {
            throw new IllegalStateException("Change job state failed: " + jobId);
        }
        if (toState == JobState.ENABLED) {
            updateNextTriggerTime(jobMapper.get(jobId));
        }
    }

    @Transactional(transactionManager = SPRING_BEAN_NAME_TX_MANAGER, rollbackFor = Exception.class)
    public void manualTriggerJob(long jobId) throws JobException {
        triggerJob(getRequiredJob(jobId), RunType.MANUAL, System.currentTimeMillis());
    }

    @Transactional(transactionManager = SPRING_BEAN_NAME_TX_MANAGER, rollbackFor = Exception.class)
    public void scheduleTriggerJob(SchedJob job, long triggerTime) throws JobException {
        if (isOneAffectedRow(jobMapper.updateNextTriggerTime(job))) {
            triggerJob(job, RunType.SCHEDULE, triggerTime);
        } else {
            LOG.warn("Schedule trigger job unsuccessful: {}", job.getJobId());
        }
    }

    /**
     * Set or clear task worker
     *
     * @param worker  the worker
     * @param taskIds the task id list
     */
    @Transactional(transactionManager = SPRING_BEAN_NAME_TX_MANAGER, rollbackFor = Exception.class)
    public void updateTaskWorker(String worker, List<Long> taskIds) {
        if (CollectionUtils.isNotEmpty(taskIds)) {
            // Sort for prevent sql deadlock: Deadlock found when trying to get lock; try restarting transaction
            taskIds = taskIds.stream().distinct().sorted().collect(Collectors.toList());
            Lists.partition(taskIds, PROCESS_BATCH_SIZE).forEach(ids -> taskMapper.batchUpdateWorker(worker, ids));
        }
    }

    // ------------------------------------------------------------------database operation within spring TransactionTemplate

    /**
     * Listen task dispatch failed event
     * <p> {@code `@Async`}需要标注{@code `@EnableAsync`}来启用，默认使用的是`SimpleAsyncTaskExecutor`线程池，会为每个任务创建一个新线程(慎用默认的线程池)
     *
     * @param event the TaskDispatchFailedEvent
     */
    @EventListener
    public void processTaskDispatchFailedEvent(TaskDispatchFailedEvent event) {
        transactionTemplate.executeWithoutResult(status -> {
            long taskId = event.getTaskId();
            if (!shouldTerminateDispatchFailedTask(taskId)) {
                return;
            }
            if (!taskMapper.terminate(taskId, null, ExecuteState.DISPATCH_FAILED, ExecuteState.WAITING, null, null)) {
                LOG.warn("Terminate dispatch failed task unsuccessful: {}", taskId);
            }
        });
    }

    /**
     * Starts the task
     *
     * @param param the start task param
     * @return start result
     */
    public StartTaskResult startTask(StartTaskParam param) {
        param.check();
        return doInSynchronizedTransaction0(param.getInstanceId(), param.getWnstanceId(), lockInstanceId -> {
            String startRequestId = param.getStartRequestId();
            LOG.info("Task trace [{}] starting: {}, {}", param.getTaskId(), param.getWorker(), startRequestId);
            Date now = new Date();
            // 如果先`get`查一次然后`start`，最后再`get`查的话数据可能会被缓存，返回`runState=10`
            // 若先`instanceMapper.lock(lockInstanceId)`，不会出现以上问题
            if (isNotAffectedRow(taskMapper.start(param.getTaskId(), param.getWorker(), startRequestId, now))) {
                if (!taskMapper.checkStartIdempotent(param.getTaskId(), param.getWorker(), startRequestId)) {
                    return StartTaskResult.failure("Start task failure.");
                }
                LOG.info("Start task idempotent: {}, {}, {}", param.getTaskId(), param.getWorker(), startRequestId);
            }
            if (isNotAffectedRow(instanceMapper.start(param.getInstanceId(), now))) {
                SchedInstance instance = instanceMapper.get(param.getInstanceId());
                Assert.state(instance != null && instance.isRunning(), () -> "Start instance failure: " + instance);
            }
            return ModelConverter.toStartTaskResult(taskMapper.get(param.getTaskId()));
        });
    }

    /**
     * Stops the task
     *
     * @param param the stop task param
     * @return {@code true} if stopped task successful
     */
    public boolean stopTask(StopTaskParam param) {
        param.check();
        Operation ops = param.getOperation();
        LOG.info("Task trace [{}] stopping: {}, {}, {}", param.getTaskId(), ops, param.getToState(), param.getWorker());
        return doInSynchronizedTransaction(param.getInstanceId(), param.getWnstanceId(), instance -> {
            Assert.isTrue(!instance.isWorkflowLead(), () -> "Stop task instance cannot be workflow lead: " + instance);
            if (instance.isTerminal()) {
                return false;
            }
            ExecuteState toState = param.getToState();
            Date executeEndTime = toState.isTerminal() ? new Date() : null;
            String errMsg = param.getErrorMsg();
            if (!taskMapper.terminate(param.getTaskId(), param.getWorker(), toState, ExecuteState.EXECUTING, executeEndTime, errMsg)) {
                // usual is worker invoke http timeout, then retry
                LOG.warn("Conflict stop executing task: {}, {}", param.getTaskId(), toState);
                return false;
            }

            List<SchedTask> tasks = taskMapper.findBaseByInstanceId(param.getInstanceId());
            if (toState == ExecuteState.WAITING) {
                Assert.isTrue(ops == Operation.SHUTDOWN_RESUME, () -> "Operation must be SHUTDOWN_RESUME, but actual: " + ops);
                if (tasks.stream().allMatch(SchedTask::isWaiting)) {
                    boolean updated = instanceMapper.updateState(param.getInstanceId(), RunState.WAITING, RunState.RUNNING);
                    Assert.isTrue(updated, () -> "Shutdown resume instance state to WAITING failed: " + param.getInstanceId());
                }
                if (!updateInstanceNextScanTime(instance, new Date(System.currentTimeMillis() + conf.getShutdownTaskDelayResumeMs()))) {
                    LOG.warn("Resume task renew instance update time failed: {}", param.getTaskId());
                }
                return true;
            }

            Tuple2<RunState, Date> tuple = obtainRunState(tasks);
            if (tuple == null) {
                // If the instance has (WAITING or EXECUTING) task
                return true;
            }

            if (!tuple.a.isTerminal()) {
                Assert.isTrue(tuple.a == RunState.PAUSED, () -> "Run state must be PAUSED, but actual: " + tuple.a);
                pauseInstance(instance.isWorkflow() ? instanceMapper.get(instance.getWnstanceId()) : instance);
                return true;
            }

            boolean updated = instanceMapper.terminate(param.getInstanceId(), tuple.a, RS_TERMINABLE, tuple.b);
            Assert.state(updated, () -> "Stop task instance failed: " + param.getInstanceId() + ", " + tuple.a);
            // the last executing task of this sched instance
            instance.markTerminated(tuple.a, tuple.b);
            if (ops.isTrigger()) {
                // trigger operation
                processTerminatedInstance(instance);
            } else if (instance.isWorkflowNode()) {
                Assert.isTrue(tuple.a == RunState.CANCELED, () -> "Invalid workflow non-trigger stop state: " + tuple.a);
                updateWorkflowNodeState(instance, tuple.a, RS_TERMINABLE);
                updateWorkflowLeadState(instanceMapper.get(instance.getWnstanceId()), tuple.a, RS_RUNNABLE);
            } else {
                Assert.isTrue(tuple.a == RunState.CANCELED, () -> "Invalid general non-trigger stop state: " + tuple.a);
                afterTerminatedInstance(instance);
            }

            return true;
        });
    }

    /**
     * Force change instance state, for example: CANCELED -> WAITING
     *
     * @param instanceId     the instance id, unsupported workflow
     * @param toExecuteState the target execute state
     */
    public void changeInstanceState(long instanceId, ExecuteState toExecuteState) {
        Assert.isTrue(toExecuteState != ExecuteState.EXECUTING, () -> "Force change state invalid target: " + toExecuteState);
        doInSynchronizedTransaction(instanceId, null, instance -> {
            Assert.isTrue(!instance.isWorkflow(), () -> "Force change state unsupported workflow: " + instanceId);
            RunState fromRunState = RunState.of(instance.getRunState());
            RunState toRunState = toExecuteState.runState();
            Assert.isTrue(fromRunState != RunState.RUNNING, "Force change state current cannot be RUNNING.");
            Assert.isTrue(fromRunState != toRunState, () -> "Force change state current cannot equals target " + toRunState);

            boolean updated = instanceMapper.updateState(instanceId, toRunState, fromRunState);
            Assert.state(updated, () -> "Force change state failed: " + instanceId);
            int changedTaskRows = taskMapper.forceChangeState(instanceId, toExecuteState.value());
            if (toExecuteState == ExecuteState.WAITING) {
                Tuple3<SchedJob, SchedInstance, List<SchedTask>> tuple = buildDispatchParam(instanceId, changedTaskRows);
                doAfterTransactionCommit(() -> dispatch(tuple.a, tuple.b, tuple.c));
            }
            LOG.info("Force change state success {}, {}", instanceId, toExecuteState);
        });
    }

    public void deleteInstance(long instanceId) {
        doInSynchronizedTransaction(instanceId, requireWnstanceIdIfWorkflow(instanceId), instance -> {
            Assert.isTrue(instance.isTerminal(), () -> "Deleting instance must be terminal: " + instance);
            if (instance.isWorkflow()) {
                Assert.isTrue(instance.isWorkflowLead(), () -> "Delete workflow instance must be lead: " + instanceId);
                List<SchedInstance> nodeInstances = instanceMapper.findWorkflowNode(instanceId);
                assertHasAffectedRow(instanceMapper.deleteByWnstanceId(instanceId), () -> "Delete workflow instance failed: " + instanceId);
                assertHasAffectedRow(workflowMapper.deleteByWnstanceId(instanceId), () -> "Delete workflow config failed: " + instanceId);
                for (SchedInstance nodeInstance : nodeInstances) {
                    int row = taskMapper.deleteByInstanceId(nodeInstance.getInstanceId());
                    assertHasAffectedRow(row, () -> "Delete workflow task failed: " + nodeInstance);
                }
            } else {
                Assert.isTrue(!instance.getRetrying(), "Cannot delete retrying original instance.");
                Assert.isTrue(!instance.isRunRetry(), "Cannot delete run retry sub instance.");
                Set<Long> instanceIds = instanceMapper.findRunRetry(instanceId)
                    .stream().map(SchedInstance::getInstanceId).collect(Collectors.toSet());
                instanceIds.add(instanceId);
                for (Long id : instanceIds) {
                    assertOneAffectedRow(instanceMapper.deleteByInstanceId(id), () -> "Delete instance failed: " + id);
                    assertHasAffectedRow(taskMapper.deleteByInstanceId(id), () -> "Delete task failed: " + id);
                }
            }
            LOG.info("Delete sched instance success {}", instanceId);
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
        LOG.info("Purge instance: {}", instanceId);
        return doInSynchronizedTransaction(instanceId, inst.getWnstanceId(), instance -> {
            Assert.isTrue(!instance.isWorkflowLead(), () -> "Purge instance cannot be workflow lead: " + instance);
            // instance run state must in (10, 20)
            if (!instance.isPausable()) {
                return false;
            }
            List<SchedTask> tasks = taskMapper.findBaseByInstanceId(instanceId);
            if (tasks.stream().anyMatch(SchedTask::isWaiting) || workerClient.hasAliveExecutingTasks(tasks)) {
                LOG.warn("Purge instance failed, has waiting or alive executing task: {}", tasks);
                return false;
            }

            Tuple2<RunState, Date> tuple = obtainRunState(tasks);
            if (tuple == null) {
                tuple = Tuple2.of(RunState.CANCELED, new Date());
            }
            Assert.isTrue(tuple.a.isTerminal(), () -> "Purge instance state must be terminal state: " + instance);
            if (!instanceMapper.terminate(instanceId, tuple.a, RS_TERMINABLE, tuple.b)) {
                throw new IllegalStateException("Purge instance failed: " + instance + ", " + tuple.a);
            }
            tasks.stream().filter(SchedTask::isPausable).forEach(e -> {
                String worker = e.isExecuting() ? Strings.requireNonBlank(e.getWorker()) : null;
                ExecuteState fromState = ExecuteState.of(e.getExecuteState());
                taskMapper.terminate(e.getTaskId(), worker, ExecuteState.EXECUTE_ABORTED, fromState, new Date(), null);
            });

            instance.markTerminated(tuple.a, tuple.b);
            processTerminatedInstance(instance);
            LOG.warn("Purge instance {} to state {}", instanceId, tuple.a);
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
        LOG.info("Pause instance: {}", instanceId);
        return doInSynchronizedTransaction(instanceId, requireWnstanceIdIfWorkflow(instanceId), instance -> {
            return doIfTrue(instance.isPausable(), () -> pauseInstance(instance));
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
        LOG.info("Cancel instance: {}, {}", instanceId, ops);
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
        LOG.info("Resume instance: {}", instanceId);
        return doInSynchronizedTransaction(instanceId, requireWnstanceIdIfWorkflow(instanceId), instance -> {
            return doIfTrue(instance.isPaused(), () -> resumeInstance(instance));
        });
    }

    // ------------------------------------------------------------------private methods

    private SchedJob getRequiredJob(long jobId) {
        return Objects.requireNonNull(jobMapper.get(jobId), () -> "Job not found: " + jobId);
    }

    private void triggerJob(SchedJob job, RunType runType, long triggerTime) throws JobException {
        TriggerInstance triggerInstance = TriggerInstance.of(this, job, null, runType, triggerTime);
        triggerInstance.save();
        doAfterTransactionCommit(triggerInstance::dispatch);
    }

    private boolean shouldTerminateDispatchFailedTask(long taskId) {
        SchedTask task = taskMapper.get(taskId);
        if (!task.isWaiting()) {
            return false;
        }
        int currentDispatchFailedCount = task.getDispatchFailedCount();
        if (currentDispatchFailedCount >= conf.getTaskDispatchFailedCountThreshold()) {
            return true;
        }
        return isOneAffectedRow(taskMapper.incrementDispatchFailedCount(taskId, currentDispatchFailedCount))
            && (currentDispatchFailedCount + 1) == conf.getTaskDispatchFailedCountThreshold();
    }

    private void updateNextTriggerTime(SchedJob job) {
        if (TriggerType.of(job.getTriggerType()) == TriggerType.DEPEND) {
            return;
        }
        Long nextTriggerTime = TriggerTimes.updateNextTriggerTime(job);
        if (!nextTriggerTime.equals(job.getNextTriggerTime())) {
            job.setNextTriggerTime(nextTriggerTime);
            assertOneAffectedRow(jobMapper.updateNextTriggerTime(job), () -> "Update next trigger time failed: " + job);
        }
    }

    private void parseTriggerConfig(SchedJob job) {
        String triggerValue = CoreUtils.trimRequired(job.getTriggerValue(), 255, "Trigger value");
        job.setTriggerValue(triggerValue);

        Long jobId = job.getJobId();
        if (TriggerType.of(job.getTriggerType()) == TriggerType.DEPEND) {
            List<Long> parentJobIds = Collects.split(triggerValue, Long::parseLong);
            Assert.notEmpty(parentJobIds, () -> "Invalid dependency parent job id config: " + triggerValue);
            Assert.isTrue(!parentJobIds.contains(jobId), () -> "Cannot depends self: " + jobId + ", " + parentJobIds);

            Map<Long, SchedJob> parentJobMap = Collects.toMap(jobMapper.findByJobIds(parentJobIds), SchedJob::getJobId);
            for (Long parentJobId : parentJobIds) {
                SchedJob parentJob = parentJobMap.get(parentJobId);
                Assert.notNull(parentJob, () -> "Parent job id not found: " + parentJobId);
                String cGroup = job.getGroup(), pGroup = parentJob.getGroup();
                Assert.isTrue(cGroup.equals(pGroup), () -> "Inconsistent depend group: " + cGroup + ", " + pGroup);
            }
            // 校验是否有循环依赖 以及 依赖层级是否太深
            checkCycleDepends(jobId, new HashSet<>(parentJobIds));

            List<SchedDepend> list = Collects.convert(parentJobIds, pid -> SchedDepend.of(pid, jobId));
            Collects.batchProcess(list, dependMapper::batchInsert, JobConstants.PROCESS_BATCH_SIZE);
            job.setTriggerValue(Joiner.on(Str.COMMA).join(parentJobIds));
            job.setNextTriggerTime(null);
        } else {
            job.setNextTriggerTime(TriggerTimes.updateNextTriggerTime(job));
        }
    }

    private void checkCycleDepends(Long jobId, Set<Long> parentJobIds) {
        Set<Long> outerDepends = parentJobIds;
        for (int i = 1; ; i++) {
            Map<Long, SchedDepend> map = Collects.toMap(dependMapper.findByChildJobIds(parentJobIds), SchedDepend::getParentJobId);
            if (MapUtils.isEmpty(map)) {
                return;
            }
            Assert.isTrue(!map.containsKey(jobId), () -> "Depends job has cycle: " + map.get(jobId));
            Assert.isTrue(i < conf.getMaximumJobDependsDepth(), () -> "Exceed depends depth: " + outerDepends);
            parentJobIds = map.keySet();
        }
    }

    private boolean dispatch(boolean isRedispatch, SchedJob job, SchedInstance instance, List<SchedTask> tasks) {
        ExecuteTaskParamBuilder builder = new ExecuteTaskParamBuilder(job, instance);
        RouteStrategy routeStrategy = RouteStrategy.of(job.getRouteStrategy());
        List<ExecuteTaskParam> list = new ArrayList<>(tasks.size());
        List<Tuple2<Worker, Long>> workload;

        if (routeStrategy.isBroadcast()) {
            for (SchedTask task : tasks) {
                Worker worker = task.worker();
                if (!workerClient.isAliveWorker(worker)) {
                    // 上游调用方有些处于事务中，有些不在事务中。因为此处的update操作非必须要求原子性，所以未加Spring事务。
                    taskMapper.terminate(task.getTaskId(), null, ExecuteState.BROADCAST_ABORTED, ExecuteState.WAITING, null, null);
                } else {
                    list.add(builder.build(Operation.TRIGGER, task.getTaskId(), instance.getTriggerTime(), worker));
                }
            }
        } else if (!isRedispatch || routeStrategy.isNotRoundRobin() || (workload = calculateWorkload(job, instance)).isEmpty()) {
            for (SchedTask task : tasks) {
                list.add(builder.build(Operation.TRIGGER, task.getTaskId(), instance.getTriggerTime(), null));
            }
        } else {
            // 轮询算法：选择分配到task最少的worker
            for (SchedTask task : tasks) {
                workload.sort(WORKLOAD_COMPARATOR);
                Tuple2<Worker, Long> first = workload.get(0);
                list.add(builder.build(Operation.TRIGGER, task.getTaskId(), instance.getTriggerTime(), first.a));
                first.b += 1;
            }
        }

        return workerClient.dispatch(job.getGroup(), list);
    }

    private List<Tuple2<Worker, Long>> calculateWorkload(SchedJob job, SchedInstance instance) {
        List<Worker> workers = workerClient.getDiscoveredWorkers(job.getGroup());
        if (CollectionUtils.isEmpty(workers)) {
            LOG.error("Not found available worker for calculate workload: {}", job.getGroup());
            return Collections.emptyList();
        }
        List<SchedTask> pausableTasks = taskMapper.findBaseByInstanceIdAndStates(instance.getInstanceId(), ES_PAUSABLE);
        Map<String, Long> workerScoreMapping = pausableTasks.stream()
            .filter(e -> StringUtils.isNotBlank(e.getWorker()))
            .collect(Collectors.groupingBy(SchedTask::getWorker, Collectors.counting()));
        return Collects.convert(workers, e -> Tuple2.of(e, workerScoreMapping.getOrDefault(e.serialize(), 0L)));
    }

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
     * 加JVM锁是为了尽量避免单节点内对数据库锁的等待及数据连接超时
     *
     * @param instanceId the instance id
     * @param wnstanceId the workflow instance id
     * @param action     the action
     * @return boolean value of action result
     */
    private boolean doInSynchronizedTransaction(long instanceId, Long wnstanceId, Predicate<SchedInstance> action) {
        return doInSynchronizedTransaction0(instanceId, wnstanceId, lockInstanceId -> {
            SchedInstance lockedInstance = instanceMapper.lock(lockInstanceId);
            Assert.notNull(lockedInstance, () -> "Locked instance not found: " + lockInstanceId);
            SchedInstance instance = (instanceId == lockInstanceId) ? lockedInstance : instanceMapper.get(instanceId);
            Assert.notNull(instance, () -> "Instance not found: " + instanceId);
            if (!Objects.equals(instance.getWnstanceId(), wnstanceId)) {
                throw new IllegalStateException("Inconsistent workflow instance id: " + wnstanceId + ", " + instance);
            }
            return action.test(instance);
        });
    }

    private <T> T doInSynchronizedTransaction0(long instanceId, Long wnstanceId, LongFunction<T> action) {
        Long lockInstanceId = wnstanceId != null ? wnstanceId : (Long) instanceId;
        synchronized (CoreUtils.INSTANCE_LOCK_POOL.intern(lockInstanceId)) {
            return transactionTemplate.execute(status -> action.apply(lockInstanceId));
        }
    }

    private Tuple2<RunState, Date> obtainRunState(List<SchedTask> tasks) {
        List<ExecuteState> states = tasks.stream().map(e -> ExecuteState.of(e.getExecuteState())).collect(Collectors.toList());
        if (states.stream().allMatch(ExecuteState::isTerminal)) {
            // executeEndTime is null: canceled task maybe never not started
            RunState toState = states.stream().anyMatch(ExecuteState::isFailure) ? RunState.CANCELED : RunState.COMPLETED;
            Date maxExecuteEndTime = tasks.stream()
                .map(SchedTask::getExecuteEndTime).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElseGet(Date::new);
            return Tuple2.of(toState, maxExecuteEndTime);
        }
        // if task has WAITING or EXECUTING state, then return null
        return states.stream().anyMatch(ExecuteState::isPausable) ? null : Tuple2.of(RunState.PAUSED, null);
    }

    private void pauseInstance(SchedInstance instance) {
        if (instance.isWorkflow()) {
            long instanceId = instance.getInstanceId();
            Assert.isTrue(instance.isWorkflowLead(), () -> "Pause instance must be workflow lead: " + instanceId);
            // pause sched_workflow running node
            instanceMapper.findWorkflowNode(instanceId).stream().filter(SchedInstance::isPausable).forEach(this::pauseInstance0);
            updateWorkflowLeadState(instance, RunState.PAUSED, RS_WAITING);
        } else {
            pauseInstance0(instance);
        }
    }

    private void pauseInstance0(SchedInstance instance) {
        Assert.isTrue(instance.isPausable(), () -> "Invalid pause instance state: " + instance);
        long instanceId = instance.getInstanceId();
        Operation ops = Operation.PAUSE;

        // update task state: (WAITING) -> (PAUSE)
        taskMapper.updateStateByInstanceId(instanceId, ops.toState().value(), ES_WAITING, null);

        // load the alive executing tasks
        List<ExecuteTaskParam> executingTasks = loadExecutingTasks(instance, ops);
        if (executingTasks.isEmpty()) {
            // has non executing task, update sched instance state
            Tuple2<RunState, Date> tuple = obtainRunState(taskMapper.findBaseByInstanceId(instanceId));
            // must be paused or terminated
            Assert.notNull(tuple, () -> "Pause instance failed: " + instanceId);
            boolean updated = instanceMapper.terminate(instanceId, tuple.a, RS_PAUSABLE, tuple.b);
            Assert.state(updated, () -> "Pause instance failed: " + instance + ", " + tuple.a);
            if (instance.isWorkflowNode()) {
                updateWorkflowNodeState(instance, tuple.a, RS_PAUSABLE);
            } else if (tuple.a.isTerminal()) {
                instance.markTerminated(tuple.a, tuple.b);
                afterTerminatedInstance(instance);
            }
        } else {
            // has alive executing tasks: dispatch and pause executing tasks
            doAfterTransactionCommit(() -> workerClient.dispatch(executingTasks));
        }
    }

    private void cancelInstance(SchedInstance instance, Operation ops) {
        if (instance.isWorkflow()) {
            long instanceId = instance.getInstanceId();
            Assert.isTrue(instance.isWorkflowLead(), () -> "Cancel instance must be workflow lead: " + instanceId);
            instanceMapper.findWorkflowNode(instanceId).stream().filter(e -> !e.isTerminal()).forEach(e -> cancelInstance0(e, ops));
            updateWorkflowLeadState(instance, RunState.CANCELED, RS_RUNNABLE);
        } else {
            cancelInstance0(instance, ops);
        }
    }

    private void cancelInstance0(SchedInstance instance, Operation ops) {
        long instanceId = instance.getInstanceId();
        // update: (WAITING or PAUSED) -> (CANCELED)
        taskMapper.updateStateByInstanceId(instanceId, ops.toState().value(), ES_EXECUTABLE, new Date());

        // load the alive executing tasks
        List<ExecuteTaskParam> executingTasks = loadExecutingTasks(instance, ops);
        if (executingTasks.isEmpty()) {
            // has non executing execute_state
            Tuple2<RunState, Date> tuple = obtainRunState(taskMapper.findBaseByInstanceId(instanceId));
            Assert.notNull(tuple, () -> "Cancel instance obtain run state failed: " + instanceId);
            // if all task paused, should update to canceled state
            if (tuple.a == RunState.PAUSED) {
                tuple = Tuple2.of(RunState.CANCELED, new Date());
            }
            if (!instanceMapper.terminate(instanceId, tuple.a, RS_TERMINABLE, tuple.b)) {
                throw new IllegalStateException("Cancel instance failed: " + instance + ", " + tuple.a);
            }
            instance.markTerminated(tuple.a, tuple.b);
            if (instance.isWorkflowNode()) {
                updateWorkflowNodeState(instance, tuple.a, RS_TERMINABLE);
            } else {
                afterTerminatedInstance(instance);
            }
        } else {
            // dispatch and cancel executing tasks
            doAfterTransactionCommit(() -> workerClient.dispatch(executingTasks));
        }
    }

    private void resumeInstance(SchedInstance instance) {
        if (instance.isWorkflow()) {
            long instanceId = instance.getInstanceId();
            Assert.isTrue(instance.isWorkflowLead(), () -> "Resume instance must be workflow lead: " + instanceId);
            // update sched_instance paused lead to running state
            boolean updated = instanceMapper.updateState(instanceId, RunState.RUNNING, RunState.PAUSED);
            Assert.state(updated, () -> "Resume workflow lead instance failed: " + instanceId);
            workflowMapper.resumeWaiting(instanceId);
            for (SchedInstance nodeInstance : instanceMapper.findWorkflowNode(instanceId)) {
                if (nodeInstance.isPaused()) {
                    resumeInstance0(nodeInstance);
                    updateWorkflowNodeState(nodeInstance, RunState.RUNNING, RS_PAUSED);
                }
            }
            WorkflowGraph graph = WorkflowGraph.of(workflowMapper.findByWnstanceId(instanceId));
            try {
                List<Runnable> dispatchActions = processWorkflowGraph(instance, graph, graph.map());
                doAfterTransactionCommit(dispatchActions);
            } catch (JobException e) {
                ExceptionUtils.rethrow(e);
            }
        } else {
            resumeInstance0(instance);
        }
    }

    private void resumeInstance0(SchedInstance instance) {
        long instanceId = instance.getInstanceId();
        boolean updated = instanceMapper.updateState(instanceId, RunState.WAITING, RunState.PAUSED);
        Assert.state(updated, () -> "Resume sched instance failed: " + instanceId);

        int row = taskMapper.updateStateByInstanceId(instanceId, ExecuteState.WAITING.value(), ES_PAUSED, null);
        assertHasAffectedRow(row, "Resume sched task failed.");

        // dispatch task
        Tuple3<SchedJob, SchedInstance, List<SchedTask>> param = buildDispatchParam(instanceId, row);
        doAfterTransactionCommit(() -> dispatch(param.a, param.b, param.c));
    }

    private void processTerminatedInstance(SchedInstance instance) {
        Assert.isTrue(!instance.isWorkflowLead(), () -> "After terminate task cannot be workflow lead: " + instance);
        RunState runState = RunState.of(instance.getRunState());

        if (runState == RunState.CANCELED) {
            retryJob(instance);
        } else if (runState == RunState.COMPLETED) {
            if (!instance.isWorkflowNode()) {
                afterTerminatedInstance(instance);
            }
            processWorkflowInstance(instance);
            dependJob(instance);
        } else {
            throw new IllegalStateException("Unknown terminate run state " + runState);
        }
    }

    private void retryJob(SchedInstance failed) {
        Long retryingInstanceId = ThrowingSupplier.doCaught(() -> retryJob0(failed));
        if (retryingInstanceId != null) {
            startRetrying(failed);
            return;
        }
        if (failed.isWorkflowNode()) {
            // If workflow without retry, then require update workflow graph state
            updateWorkflowNodeState(failed, RunState.CANCELED, RS_TERMINABLE);
            updateWorkflowLeadState(instanceMapper.get(failed.getWnstanceId()), RunState.CANCELED, RS_RUNNABLE);
        } else {
            afterTerminatedInstance(failed);
        }
    }

    private Long retryJob0(SchedInstance failed) throws JobException {
        SchedJob job = getRequiredJob(failed.getJobId());
        int retriedCount = failed.obtainRetriedCount();
        if (!job.retryable(RunState.of(failed.getRunState()), retriedCount)) {
            return null;
        }

        // build retry instance
        long retryInstanceId = generateId();
        long triggerTime = job.computeRetryTriggerTime(++retriedCount);
        SchedInstance retryInstance = SchedInstance.of(failed, retryInstanceId, job.getJobId(), RunType.RETRY, triggerTime, retriedCount);
        retryInstance.setWorkflowCurNode(failed.getWorkflowCurNode());
        // build retry tasks
        List<SchedTask> tasks = splitRetryTask(job, failed, retryInstance);
        if (tasks.isEmpty()) {
            // the broadcast failed task worker all dead then terminate retry
            LOG.warn("Retry instance split tasks is empty: {}, {}", job, failed);
            return null;
        }

        ThrowingSupplier<Runnable, Throwable> persistenceAction = () -> {
            if (failed.isWorkflowNode()) {
                // 如果是workflow，则需要更新sched_workflow.instance_id
                String curNode = failed.getWorkflowCurNode();
                int row = workflowMapper.update(failed.getWnstanceId(), curNode, null, retryInstanceId, RS_RUNNING, failed.getInstanceId());
                assertHasAffectedRow(row, () -> "Retry instance, workflow node update failed.");
            }
            saveInstanceAndTasks(retryInstance, tasks);
            return () -> dispatch(job, retryInstance, tasks);
        };
        Consumer<Throwable> errorHandler = t -> { throw new IllegalStateException("Create retry instance failed: " + failed, t); };
        // 使用嵌套事务：保证`workflow & instance & tasks`操作的原子性，异常则回滚而不影响外层事务
        Runnable dispatchAction = doInNestedTransaction(transactionTemplate, persistenceAction, errorHandler);
        doAfterTransactionCommit(dispatchAction);

        return retryInstanceId;
    }

    private List<SchedTask> splitRetryTask(SchedJob job, SchedInstance failed, SchedInstance retry) throws JobException {
        RetryType retryType = RetryType.of(job.getRetryType());
        if (retryType == RetryType.ALL) {
            // re-split job
            SplitJobParam splitJobParam;
            if (failed.isWorkflow()) {
                List<PredecessorInstance> list = loadWorkflowPredecessorInstances(job, failed.getWnstanceId(), failed.getInstanceId());
                splitJobParam = ModelConverter.toSplitJobParam(job, retry, list);
            } else {
                splitJobParam = ModelConverter.toSplitJobParam(job, retry);
            }
            return splitJob(job.getGroup(), retry.getInstanceId(), splitJobParam);
        } else if (retryType == RetryType.FAILED) {
            return taskMapper.findLargeByInstanceId(failed.getInstanceId())
                .stream()
                .filter(SchedTask::isFailure)
                // Broadcast task must be retried with the same worker
                .filter(e -> !job.isBroadcast() || workerClient.isAliveWorker(e.worker()))
                .map(e -> SchedTask.of(e.getTaskParam(), generateId(), retry.getInstanceId(), e.getTaskNo(), e.getTaskCount(), e.getWorker()))
                .collect(Collectors.toList());
        } else {
            throw new UnsupportedOperationException("Retry instance, unknown retry type: " + job.getJobId() + ", " + retryType);
        }
    }

    private void dependJob(SchedInstance parent) {
        if (parent.isWorkflowNode() || !parent.isCompleted()) {
            return;
        }
        for (SchedDepend depend : dependMapper.findByParentJobId(parent.getJobId())) {
            ThrowingRunnable.doCaught(() -> dependJob(parent, depend), () -> "Depend job error: " + parent + ", " + depend);
        }
    }

    private void dependJob(SchedInstance parent, SchedDepend depend) throws JobException {
        SchedJob childJob = getRequiredJob(depend.getChildJobId());
        if (childJob.isDisabled()) {
            LOG.warn("Depend child job disabled: {}", childJob);
            return;
        }

        // 使用嵌套事务：保证`save`方法内部数据操作的原子性，异常则回滚而不影响外层事务
        TriggerInstance dependInstance = TriggerInstance.of(this, childJob, parent, RunType.DEPEND, System.currentTimeMillis());
        Consumer<Throwable> errorHandler = t -> LOG.error("Create depend instance failed: {}, {}", childJob, parent, t);
        ThrowingSupplier<Runnable, Throwable> persistenceAction = () -> { dependInstance.save(); return dependInstance::dispatch; };
        Runnable dispatchAction = doInNestedTransaction(transactionTemplate, persistenceAction, errorHandler);
        doAfterTransactionCommit(dispatchAction);
    }

    private List<ExecuteTaskParam> loadExecutingTasks(SchedInstance instance, Operation ops) {
        List<ExecuteTaskParam> executingTasks = new ArrayList<>();
        ExecuteTaskParamBuilder builder = null;
        long triggerTime = System.currentTimeMillis();
        for (SchedTask task : taskMapper.findBaseByInstanceIdAndStates(instance.getInstanceId(), ES_EXECUTING)) {
            Worker worker = task.worker();
            if (workerClient.isAliveWorker(worker)) {
                if (builder == null) {
                    builder = new ExecuteTaskParamBuilder(getRequiredJob(instance.getJobId()), instance);
                }
                executingTasks.add(builder.build(ops, task.getTaskId(), triggerTime, worker));
            } else {
                // update dead task
                Date executeEndTime = ops.toState().isTerminal() ? new Date() : null;
                ExecuteState toState = ops.toState().isTerminal() ? ExecuteState.EXECUTE_ABORTED : ops.toState();
                ExecuteState fromState = ExecuteState.EXECUTING;
                if (taskMapper.terminate(task.getTaskId(), task.getWorker(), toState, fromState, executeEndTime, null)) {
                    LOG.info("Terminate dead worker executing task success: {}", task);
                } else {
                    LOG.error("Terminate dead worker executing task failed: {}", task);
                }
            }
        }
        return executingTasks;
    }

    private Tuple3<SchedJob, SchedInstance, List<SchedTask>> buildDispatchParam(long instanceId, int expectTaskSize) {
        SchedInstance instance = instanceMapper.get(instanceId);
        SchedJob job = getRequiredJob(instance.getJobId());
        List<SchedTask> waitingTasks = taskMapper.findLargeByInstanceIdAndStates(instanceId, ES_WAITING);
        int size = waitingTasks.size();
        Assert.state(size == expectTaskSize, () -> "Invalid dispatch tasks size: " + size + ", " + expectTaskSize);
        return Tuple3.of(job, instance, waitingTasks);
    }

    private void startRetrying(SchedInstance instance) {
        if (!instance.isRunRetry()) {
            RunState state = RunState.CANCELED;
            boolean updated = instanceMapper.updateRetrying(instance.getInstanceId(), true, state, state);
            Assert.state(updated, () -> "Start retrying failed: " + instance);
        }
    }

    private void stopRetrying(SchedInstance instance, RunState toState) {
        if (instance.isRunRetry()) {
            long id = instance.obtainRetryOriginalInstanceId();
            boolean updated = instanceMapper.updateRetrying(id, false, toState, RunState.CANCELED);
            Assert.state(updated, () -> "Stop retrying failed: " + toState + ", " + instance);
        }
    }

    private void afterTerminatedInstance(SchedInstance instance) {
        Assert.isTrue(instance.isTerminal(), () -> "Renew fixed instance must be terminal state: " + instance);
        Assert.isTrue(!instance.isWorkflowNode(), () -> "Renew fixed instance cannot be workflow node: " + instance);
        if (instance.isRunRetry()) {
            stopRetrying(instance, RunState.of(instance.getRunState()));
        }

        // 1、get original instance if run retry
        SchedInstance original = instance.isRunRetry() ? instanceMapper.get(instance.getPnstanceId()) : instance;
        SchedJob job = jobMapper.get(original.getJobId());
        if (job == null) {
            LOG.error("Sched job not found: {}", original);
            return;
        }

        // 2、if fixed trigger type job then update the job next triggertime
        if (job.isEnabled() && job.isFixedTriggerType() && original.isRunSchedule()) {
            TriggerType triggerType = TriggerType.of(job.getTriggerType());
            long lastTriggerTime = original.getTriggerTime(), nextTriggerTime;
            if (triggerType == TriggerType.FIXED_RATE) {
                Date time = triggerType.computeNextTriggerTime(job.getTriggerValue(), new Date(original.getTriggerTime()));
                nextTriggerTime = Dates.max(time, original.getRunEndTime()).getTime();
            } else {
                // TriggerType.FIXED_DELAY
                nextTriggerTime = triggerType.computeNextTriggerTime(job.getTriggerValue(), original.getRunEndTime()).getTime();
            }
            boolean updated = isOneAffectedRow(jobMapper.updateFixedNextTriggerTime(job.getJobId(), lastTriggerTime, nextTriggerTime));
            LOG.info("Renew fixed next trigger time: {}, {}, {}, {}", job.getJobId(), lastTriggerTime, nextTriggerTime, updated);
        }

        // 3、alert instance event
        if (alerter != null) {
            AlertInstanceEvent event = ModelConverter.toAlertInstanceEvent(job, original, instance);
            doAfterTransactionCommit(() -> alerter.alert(event));
        }
    }

    // ------------------------------------------------------------------private workflow methods

    private void updateWorkflowNodeState(SchedInstance node, RunState toState, List<Integer> fromStates) {
        Assert.isTrue(node.isWorkflowNode(), () -> "Update workflow cur node state must be node: " + node);
        String curNode = node.getWorkflowCurNode();
        int row = workflowMapper.update(node.getWnstanceId(), curNode, toState.value(), null, fromStates, node.getInstanceId());
        assertHasAffectedRow(row, () -> "Update workflow state failed: " + node + ", " + toState);
        if (toState.isTerminal()) {
            stopRetrying(node, toState);
        }
    }

    private void updateWorkflowLeadState(SchedInstance lead, RunState toState, List<Integer> fromStates) {
        Assert.isTrue(lead.isWorkflowLead(), () -> "Update workflow free node state must be lead: " + lead);
        long wnstanceId = lead.getWnstanceId();
        workflowMapper.update(wnstanceId, null, toState.value(), null, fromStates, null);
        stopWorkflowGraph(wnstanceId, WorkflowGraph.of(workflowMapper.findByWnstanceId(wnstanceId)));
    }

    private void processWorkflowInstance(SchedInstance node) {
        if (!node.isWorkflowNode()) {
            return;
        }
        // update current node state
        updateWorkflowNodeState(node, RunState.COMPLETED, RS_TERMINABLE);

        // if terminal all, then update workflow nodes
        long wnstanceId = node.getWnstanceId();
        WorkflowGraph graph = WorkflowGraph.of(workflowMapper.findByWnstanceId(wnstanceId));
        if (stopWorkflowGraph(wnstanceId, graph)) {
            return;
        }

        // process next workflow node
        Map<DAGEdge, SchedWorkflow> map = graph.successors(node.parseWorkflowCurNode());
        SchedInstance lead = instanceMapper.get(wnstanceId);

        Consumer<Throwable> errorHandler = t -> {
            LOG.error("Process workflow node error: {}", node, t);
            updateWorkflowLeadState(lead, RunState.CANCELED, RS_RUNNABLE);
        };
        // 使用嵌套事务：保证`processWorkflowNode`方法内部数据操作的原子性，异常则回滚而不影响外层事务
        ThrowingSupplier<List<Runnable>, Throwable> persistenceAction = () -> processWorkflowGraph(lead, graph, map);
        List<Runnable> dispatchActions = doInNestedTransaction(transactionTemplate, persistenceAction, errorHandler);
        doAfterTransactionCommit(dispatchActions);
    }

    private List<Runnable> processWorkflowGraph(SchedInstance lead, WorkflowGraph graph,
                                                Map<DAGEdge, SchedWorkflow> map) throws JobException {
        Assert.isTrue(lead.isWorkflowLead(), () -> "Process workflow node must be lead: " + lead);
        List<Runnable> dispatchActions = new ArrayList<>();
        if (!map.isEmpty()) {
            SchedJob job = getRequiredJob(lead.getJobId());
            Set<DAGNode> duplicates = new HashSet<>();
            for (Map.Entry<DAGEdge, SchedWorkflow> edge : map.entrySet()) {
                processWorkflowGraph(dispatchActions, job, lead, graph, duplicates, edge);
            }
        }
        return dispatchActions;
    }

    private void processWorkflowGraph(List<Runnable> dispatchActions, SchedJob job, SchedInstance lead, WorkflowGraph graph,
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
        RunType runType = RunType.of(lead.getRunType());
        SchedWorkflow lastPredecessor = predecessors.stream().max(BaseEntity.UPDATED_AT_COMPARATOR).orElse(null);
        SchedInstance parent = (lastPredecessor == null) ? lead : instanceMapper.get(lastPredecessor.getInstanceId());
        SchedInstance nextInstance = SchedInstance.of(parent, nextInstanceId, job.getJobId(), runType, System.currentTimeMillis(), 0);
        nextInstance.setWorkflowCurNode(workflow.getCurNode());

        int row = workflowMapper.update(wnstanceId, workflow.getCurNode(), RunState.RUNNING.value(), nextInstanceId, RS_WAITING, null);
        assertHasAffectedRow(row, () -> "Start workflow node failed: " + workflow);

        List<PredecessorInstance> list = predecessors.isEmpty() ? null : loadWorkflowPredecessorInstances(job, wnstanceId, nextInstanceId);
        SplitJobParam splitJobParam = ModelConverter.toSplitJobParam(job, nextInstance, list);
        List<SchedTask> tasks = splitJob(job.getGroup(), nextInstanceId, splitJobParam);

        // save to db
        saveInstanceAndTasks(nextInstance, tasks);
        dispatchActions.add(() -> dispatch(job, nextInstance, tasks));
    }

    private boolean stopWorkflowGraph(long wnstanceId, WorkflowGraph graph) {
        if (graph.anyMatch(e -> e.getKey().getTarget().isEnd() && !e.getValue().isTerminal())) {
            // if end node is not terminal state, then process the end node run state
            Map<DAGEdge, SchedWorkflow> ends = graph.predecessors(DAGNode.END);
            if (ends.values().stream().allMatch(SchedWorkflow::isTerminal)) {
                RunState endState = ends.values().stream().anyMatch(SchedWorkflow::isFailure) ? RunState.CANCELED : RunState.COMPLETED;
                int row = workflowMapper.update(wnstanceId, DAGNode.END.toString(), endState.value(), null, RS_TERMINABLE, null);
                assertHasAffectedRow(row, () -> "Update workflow end node failed: " + wnstanceId + ", " + endState);
                ends.forEach((k, v) -> graph.get(k.getTarget(), DAGNode.END).setRunState(endState.value()));
            }
        }
        if (graph.allMatch(e -> e.getValue().isTerminal())) {
            // terminate lead instance
            RunState state = graph.anyMatch(e -> e.getValue().isFailure()) ? RunState.CANCELED : RunState.COMPLETED;
            boolean updated = instanceMapper.terminate(wnstanceId, state, RS_TERMINABLE, new Date());
            Assert.state(updated, () -> "Stop workflow instance failed: " + wnstanceId + ", " + state);
            SchedInstance lead = instanceMapper.get(wnstanceId);
            dependJob(lead);
            afterTerminatedInstance(lead);
            return true;
        }
        if (graph.allMatch(e -> e.getValue().isTerminal() || e.getValue().isPaused())) {
            // At Least one paused and others is terminal
            boolean updated = instanceMapper.updateState(wnstanceId, RunState.PAUSED, RunState.RUNNING);
            Assert.state(updated, () -> "Update workflow pause state failed: " + wnstanceId);
            return true;
        }
        return false;
    }

    private List<PredecessorInstance> loadWorkflowPredecessorInstances(SchedJob job, long wnstanceId, Long instanceId) {
        List<SchedWorkflow> workflows = workflowMapper.findByWnstanceId(wnstanceId);
        SchedWorkflow curWorkflow = workflows.stream().filter(e -> instanceId.equals(e.getInstanceId())).findAny().orElse(null);
        Assert.state(curWorkflow != null, () -> "Not found current workflow node: " + wnstanceId + ", " + instanceId);
        Map<DAGEdge, SchedWorkflow> predecessors = WorkflowGraph.of(workflows).predecessors(curWorkflow.parseCurNode());
        if (predecessors.isEmpty()) {
            return null;
        }
        RetryType retryType = RetryType.of(job.getRetryType());
        return Collects.convert(predecessors.values(), e -> {
            // predecessor instance下的task是全部执行成功的
            List<SchedTask> tasks = taskMapper.findLargeByInstanceId(e.getInstanceId());
            SchedInstance prev;
            if (retryType == RetryType.FAILED && (prev = instanceMapper.get(e.getInstanceId())).isRunRetry()) {
                Set<Long> instanceIds = instanceMapper.findChildren(prev.getPnstanceId(), RunType.RETRY.value())
                    .stream()
                    .map(SchedInstance::getInstanceId)
                    .filter(t -> !Objects.equals(t, e.getInstanceId()))
                    .collect(Collectors.toSet());
                instanceIds.add(prev.getPnstanceId());
                instanceIds.forEach(t -> tasks.addAll(taskMapper.findLargeByInstanceIdAndStates(t, ES_COMPLETED)));
            }
            tasks.sort(SchedTask.TASK_NO_COMPARATOR);
            return ModelConverter.toPredecessorInstance(e, tasks);
        });
    }

}
