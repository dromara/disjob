/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.manager;

import cn.ponfee.scheduler.common.base.IdGenerator;
import cn.ponfee.scheduler.common.base.LazyLoader;
import cn.ponfee.scheduler.common.base.Symbol.Str;
import cn.ponfee.scheduler.common.base.tuple.Tuple3;
import cn.ponfee.scheduler.common.spring.RpcController;
import cn.ponfee.scheduler.common.spring.TransactionUtils;
import cn.ponfee.scheduler.common.util.Collects;
import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.SupervisorService;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.enums.*;
import cn.ponfee.scheduler.core.exception.JobException;
import cn.ponfee.scheduler.core.model.SchedDepend;
import cn.ponfee.scheduler.core.model.SchedInstance;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.core.param.TaskWorker;
import cn.ponfee.scheduler.dispatch.TaskDispatcher;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import cn.ponfee.scheduler.supervisor.base.WorkerServiceClient;
import cn.ponfee.scheduler.supervisor.dao.mapper.SchedDependMapper;
import cn.ponfee.scheduler.supervisor.dao.mapper.SchedInstanceMapper;
import cn.ponfee.scheduler.supervisor.dao.mapper.SchedJobMapper;
import cn.ponfee.scheduler.supervisor.dao.mapper.SchedTaskMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.math.IntMath;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static cn.ponfee.scheduler.supervisor.base.AbstractDataSourceConfig.TX_MANAGER_SUFFIX;
import static cn.ponfee.scheduler.supervisor.base.AbstractDataSourceConfig.TX_TEMPLATE_SUFFIX;
import static cn.ponfee.scheduler.supervisor.dao.SupervisorDataSourceConfig.DB_NAME;

/**
 * Manage Schedule job.
 *
 * <p>Spring事务提交后执行一些后置操作
 *
 * <pre>方案1: {@code
 *  TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
 *      @Override
 *      public void afterCommit() {
 *          dispatch(job, instance, tasks);
 *      }
 *  });
 * }</pre>
 *
 * <pre>方案2: {@code
 *  @Resource
 *  private ApplicationEventPublisher eventPublisher;
 *
 *  private static class DispatchTaskEvent extends ApplicationEvent {
 *      public DispatchTaskEvent(Runnable source) {
 *          super(source);
 *      }
 *  }
 *
 *  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
 *  private void handle(DispatchTaskEvent event) {
 *      ((Runnable) event.getSource()).run();
 *  }
 *
 *  {
 *    // some database operation code ...
 *    eventPublisher.publishEvent(new DispatchTaskEvent(() -> dispatch(job, instance, tasks)));
 *    // others operation code ...
 *  }
 * }</pre>
 *
 * @author Ponfee
 */
@Component
public class SchedulerJobManager extends AbstractJobManager implements SupervisorService, RpcController {

    private static final String TX_MANAGER_NAME = DB_NAME + TX_MANAGER_SUFFIX;
    private static final int AFFECTED_ONE_ROW = 1;
    private static final String DEFAULT_USER = "0";
    private static final List<Integer> CANCELABLE_RUN_STATE_LIST = Collects.convert(RunState.CANCELABLE_LIST, RunState::value);
    private static final List<Integer> EXECUTABLE_EXECUTE_STATE_LIST = Collects.convert(ExecuteState.EXECUTABLE_LIST, ExecuteState::value);

    private final TransactionTemplate transactionTemplate;
    private final SchedJobMapper jobMapper;
    private final SchedInstanceMapper instanceMapper;
    private final SchedTaskMapper taskMapper;
    private final SchedDependMapper dependMapper;

    public SchedulerJobManager(IdGenerator idGenerator,
                               SupervisorRegistry discoveryWorker,
                               TaskDispatcher taskDispatcher,
                               WorkerServiceClient workerServiceClient,
                               @Qualifier(DB_NAME + TX_TEMPLATE_SUFFIX) TransactionTemplate transactionTemplate,
                               SchedJobMapper jobMapper,
                               SchedInstanceMapper instanceMapper,
                               SchedTaskMapper taskMapper,
                               SchedDependMapper dependMapper) {
        super(idGenerator, discoveryWorker, taskDispatcher, workerServiceClient);
        this.transactionTemplate = transactionTemplate;
        this.jobMapper = jobMapper;
        this.instanceMapper = instanceMapper;
        this.taskMapper = taskMapper;
        this.dependMapper = dependMapper;
    }

    // ------------------------------------------------------------------database query

    @Override
    public SchedJob getJob(long jobId) {
        return jobMapper.getByJobId(jobId);
    }

    public SchedInstance getInstance(long instanceId) {
        return instanceMapper.getByInstanceId(instanceId);
    }

    @Override
    public SchedTask getTask(long taskId) {
        return taskMapper.getByTaskId(taskId);
    }

    public List<SchedTask> findMediumTaskByInstanceId(long instanceId) {
        return taskMapper.findMediumByInstanceId(instanceId);
    }

    public List<SchedTask> findLargeTaskByInstanceId(long instanceId) {
        return taskMapper.findLargeByInstanceId(instanceId);
    }

    /**
     * Scan will be triggering sched jobs.
     *
     * @param maxNextTriggerTime the maxNextTriggerTime
     * @param size               the size
     * @return will be triggering sched jobs
     */
    public List<SchedJob> findBeTriggering(long maxNextTriggerTime, int size) {
        return jobMapper.findBeTriggering(maxNextTriggerTime, size);
    }

    public List<SchedInstance> findExpireWaiting(Date expireTime, int size) {
        return instanceMapper.findExpireState(RunState.WAITING.value(), expireTime.getTime(), expireTime, size);
    }

    public List<SchedInstance> findExpireRunning(Date expireTime, int size) {
        return instanceMapper.findExpireState(RunState.RUNNING.value(), expireTime.getTime(), expireTime, size);
    }

    public SchedInstance getByTriggerTime(long jobId, long triggerTime, int runType) {
        return instanceMapper.getByTriggerTime(jobId, triggerTime, runType);
    }

    public List<SchedInstance> findUnterminatedRetry(long instanceId) {
        return instanceMapper.findUnterminatedRetry(instanceId);
    }

    // ------------------------------------------------------------------database single operation without @Transactional annotation

    @Override
    public boolean checkpoint(long taskId, String executeSnapshot) {
        return taskMapper.checkpoint(taskId, executeSnapshot) == AFFECTED_ONE_ROW;
    }

    public boolean renewUpdateTime(SchedInstance instance, Date updateTime) {
        return instanceMapper.renewUpdateTime(instance.getInstanceId(), updateTime, instance.getVersion()) == AFFECTED_ONE_ROW;
    }

    public boolean changeJobState(long jobId, JobState to) {
        return jobMapper.updateState(jobId, to.value(), 1 ^ to.value()) == AFFECTED_ONE_ROW;
    }

    /**
     * Stop job
     *
     * @param job the job
     * @return if {@code true} update success
     */
    public boolean stopJob(SchedJob job) {
        return AFFECTED_ONE_ROW == jobMapper.stop(job);
    }

    public boolean updateNextTriggerTime(SchedJob job) {
        return jobMapper.updateNextTriggerTime(job) == AFFECTED_ONE_ROW;
    }

    public boolean updateNextScanTime(long jobId, Date nextScanTime, int version) {
        return jobMapper.updateNextScanTime(jobId, nextScanTime, version) == AFFECTED_ONE_ROW;
    }

    // ------------------------------------------------------------------operation within @Transactional annotation

    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    public void addJob(SchedJob job) {
        job.verifyBeforeAdd();

        super.verifyJobHandler(job);
        job.checkAndDefaultSetting();

        job.setJobId(generateId());
        Date now = new Date();
        parseTriggerConfig(job, now);

        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        job.setCreatedBy(DEFAULT_USER);
        job.setUpdatedBy(DEFAULT_USER);
        jobMapper.insert(job);
    }

    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    public void updateJob(SchedJob job) {
        job.verifyBeforeUpdate();

        if (StringUtils.isEmpty(job.getJobHandler())) {
            Assert.isTrue(StringUtils.isEmpty(job.getJobParam()), "Job param must be null if not set job handler.");
        } else {
            super.verifyJobHandler(job);
        }

        job.checkAndDefaultSetting();

        SchedJob dbSchedJob = jobMapper.getByJobId(job.getJobId());
        Assert.notNull(dbSchedJob, () -> "Sched job id not found " + job.getJobId());
        job.setNextTriggerTime(dbSchedJob.getNextTriggerTime());

        Date now = new Date();
        if (job.getTriggerType() == null) {
            Assert.isNull(job.getTriggerValue(), "Trigger value must be null if not set trigger type.");
        } else {
            Assert.notNull(job.getTriggerValue(), "Trigger value cannot be null if has set trigger type.");
            // update last trigger time or depends parent job id
            dependMapper.deleteByChildJobId(job.getJobId());
            parseTriggerConfig(job, now);
        }

        job.setUpdatedAt(now);
        job.setUpdatedBy(DEFAULT_USER);
        Assert.state(jobMapper.updateByJobId(job) == AFFECTED_ONE_ROW, "Update sched job fail or conflict.");
    }

    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    public void deleteJob(long jobId) {
        Assert.isTrue(jobMapper.deleteByJobId(jobId) == AFFECTED_ONE_ROW, "Delete sched job fail or conflict.");
        dependMapper.deleteByParentJobId(jobId);
        dependMapper.deleteByChildJobId(jobId);
    }

    public void deleteInstance(long instanceId) {
        doTransactionInSynchronized(instanceId, () -> {
            Integer state = instanceMapper.lockAndGetState(instanceId);
            Assert.notNull(state, () -> "Sched instance not found: " + instanceId);

            RunState runState = RunState.of(state);
            Assert.isTrue(runState.isTerminal(), () -> "Cannot delete unterminated sched instance: " + instanceId + ", run state=" + runState);

            int row = instanceMapper.deleteByInstanceId(instanceId);
            Assert.isTrue(row == AFFECTED_ONE_ROW, () -> "Delete sched instance conflict: " + instanceId);

            taskMapper.deleteByInstanceId(instanceId);
        });
    }

    public void forceUpdateInstanceState(long instanceId, int instanceTargetState, int taskTargetState) {
        ExecuteState taskTargetState0 = ExecuteState.of(taskTargetState);
        Assert.isTrue(
            taskTargetState0.runState() == RunState.of(instanceTargetState),
            () -> "Inconsistent state: " + instanceTargetState + ", " + taskTargetState
        );
        doTransactionInSynchronized(instanceId, () -> {
            Long id = instanceMapper.lockAndGetId(instanceId);
            Assert.notNull(id, () -> "Sched instance not found: " + instanceId);
            int row = instanceMapper.forceUpdateState(instanceId, instanceTargetState);
            Assert.isTrue(row == AFFECTED_ONE_ROW, () -> "Sched instance state update failed " + instanceId);

            row = taskMapper.forceUpdateState(instanceId, taskTargetState);
            Assert.isTrue(row >= AFFECTED_ONE_ROW, () -> "Sched task state update failed, instance_id=" + instanceId);

            if (taskTargetState0 == ExecuteState.WAITING) {
                Tuple3<SchedJob, SchedInstance, List<SchedTask>> params = buildDispatchParams(instanceId, row);
                TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(params.a, params.b, params.c));
            }
        });
    }

    /**
     * Manual trigger the sched job
     *
     * @param jobId the job id
     * @throws JobException if occur error
     */
    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    public void triggerJob(long jobId) throws JobException {
        SchedJob job = jobMapper.getByJobId(jobId);
        Assert.notNull(job, () -> "Sched job not found: " + jobId);

        // 1、build sched instance and sched task list
        Date now = new Date();
        SchedInstance instance = SchedInstance.create(generateId(), job.getJobId(), RunType.MANUAL, now.getTime(), 0, now);
        List<SchedTask> tasks = splitTasks(job, instance.getInstanceId(), now);

        // 2、save sched trace and sched task to database
        int row = instanceMapper.insert(instance);
        Assert.state(row == AFFECTED_ONE_ROW, () -> "Insert sched instance fail: " + instance);

        batchInsertTask(tasks);

        // 3、dispatch job task
        TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(job, instance, tasks));
    }

    /**
     * Update sched job, and save one sched instance and many tasks.
     *
     * @param job      the job
     * @param instance the instance
     * @param tasks    the tasks
     * @return if {@code true} operated success
     */
    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    public boolean updateAndSave(SchedJob job, SchedInstance instance, List<SchedTask> tasks) {
        int row = jobMapper.updateNextTriggerTime(job);
        if (row == 0) {
            // conflict operation, need not process
            return false;
        }

        row = instanceMapper.insert(instance);
        Assert.state(row == AFFECTED_ONE_ROW, () -> "Insert sched instance fail: " + instance);

        batchInsertTask(tasks);
        return true;
    }

    /**
     * Set or clear task worker
     *
     * @param list the list
     * @return {@code true} if updated at least one row
     */
    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    @Override
    public boolean updateTaskWorker(List<TaskWorker> list) {
        List<List<TaskWorker>> partition = list.size() <= JobConstants.PROCESS_BATCH_SIZE
            ? Collections.singletonList(list)
            : Lists.partition(list, JobConstants.PROCESS_BATCH_SIZE);

        return partition.stream().mapToInt(taskMapper::batchUpdateWorker).sum() >= AFFECTED_ONE_ROW;
    }

    /**
     * Starts the task.
     *
     * @param param the execution param
     * @return {@code true} if start successfully
     */
    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    @Override
    public boolean startTask(ExecuteParam param) {
        Integer state = instanceMapper.getStateByInstanceId(param.getInstanceId());
        Assert.state(state != null, () -> "Sched instance not found: " + param);
        RunState runState = RunState.of(state);
        // sched_instance.run_state must in (WAITING, RUNNING)
        Assert.state(RunState.PAUSABLE_LIST.contains(runState), () -> "Start instance failed: " + param + ", " + runState);

        Date now = new Date();
        // start sched instance(also possibly started by other task)
        int instanceRow = instanceMapper.start(param.getInstanceId(), now);

        // start sched task
        int taskRow = taskMapper.start(param.getTaskId(), param.getWorker().toString(), now);

        if (instanceRow == 0 && taskRow == 0) {
            // conflict: the task executed by other executor
            return false;
        } else {
            Assert.state(taskRow == AFFECTED_ONE_ROW, () -> "Start task failed: " + param);
            return true;
        }
    }

    /**
     * Terminate the executing task.
     *
     * @param param    the taskParam
     * @param toState  the toState
     * @param errorMsg the errorMsg
     * @return {@code true} if terminated task successful
     */
    @Override
    public boolean terminateExecutingTask(ExecuteParam param, ExecuteState toState, String errorMsg) {
        return doTransactionInSynchronized(param.getInstanceId(), () -> {
            Integer state = instanceMapper.lockAndGetState(param.getInstanceId());
            Assert.notNull(state, () -> "Terminate failed, instance_id not found: " + param.getInstanceId());
            if (RunState.of(state).isTerminal()) {
                // already terminated
                return false;
            }
            if (taskMapper.terminate(param.getTaskId(), toState.value(), ExecuteState.EXECUTING.value(), new Date(), errorMsg) == 0) {
                log.warn("Conflict terminate executing task {}, {}", param.getTaskId(), toState);
                return false;
            }

            List<SchedTask> tasks = taskMapper.findMediumByInstanceId(param.getInstanceId());
            List<ExecuteState> taskStateList = tasks.stream().map(e -> ExecuteState.of(e.getExecuteState())).collect(Collectors.toList());
            if (!taskStateList.stream().allMatch(ExecuteState::isTerminal)) {
                return true;
            }

            // the last executing task of this sched instance
            Date runEndTime = tasks.stream().map(SchedTask::getExecuteEndTime).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElseGet(Date::new);
            RunState runState = taskStateList.stream().allMatch(ExecuteState.FINISHED::equals) ? RunState.FINISHED : RunState.CANCELED;
            if (instanceMapper.terminate(param.getInstanceId(), runState.value(), CANCELABLE_RUN_STATE_LIST, runEndTime) == AFFECTED_ONE_ROW) {
                terminatePostProcess(param.getInstanceId(), runState);
            }

            return true;
        });
    }

    /**
     * Terminate the dead instance.
     *
     * @param instanceId the instanceId
     * @return if {@code true} terminate successfully
     */
    public boolean terminateDeadInstance(long instanceId) {
        return doTransactionInSynchronized(instanceId, () -> {
            Integer state = instanceMapper.lockAndGetState(instanceId);
            Assert.notNull(state, () -> "Terminate failed, instance_id not found: " + instanceId);
            if (RunState.of(state).isTerminal()) {
                // already terminated
                return false;
            }

            List<SchedTask> tasks = taskMapper.findMediumByInstanceId(instanceId);
            if (CollectionUtils.isEmpty(tasks)) {
                log.error("Not found sched instance task data {}", instanceId);
                return false;
            }

            Date runEndTime;
            RunState runState;
            List<ExecuteState> taskStateList = tasks.stream().map(e -> ExecuteState.of(e.getExecuteState())).collect(Collectors.toList());
            if (taskStateList.stream().allMatch(ExecuteState::isTerminal)) {
                // executeEndTime is null: canceled task maybe not started
                runEndTime = tasks.stream().map(SchedTask::getExecuteEndTime).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElseGet(Date::new);
                runState = taskStateList.stream().allMatch(ExecuteState.FINISHED::equals) ? RunState.FINISHED : RunState.CANCELED;
            } else if (taskStateList.stream().allMatch(e -> e == ExecuteState.PAUSED || e.isTerminal())) {
                runEndTime = null;
                runState = RunState.PAUSED;
            } else {
                runEndTime = new Date();
                runState = RunState.CANCELED;
            }
            if (instanceMapper.terminate(instanceId, runState.value(), CANCELABLE_RUN_STATE_LIST, runEndTime) != AFFECTED_ONE_ROW) {
                return false;
            }

            tasks.stream()
                .filter(e -> !ExecuteState.of(e.getExecuteState()).isTerminal())
                .forEach(e -> taskMapper.terminate(e.getTaskId(), ExecuteState.EXECUTE_TIMEOUT.value(), e.getExecuteState(), new Date(), null));

            terminatePostProcess(instanceId, runState);

            return true;
        });
    }

    // ------------------------------------------------------------------manual pause

    /**
     * Pause task by sched instance id
     *
     * @param instanceId the instance id
     * @return {@code true} if paused successfully
     */
    @Override
    public boolean pauseInstance(long instanceId) {
        return doTransactionInSynchronized(instanceId, () -> {
            Integer state = instanceMapper.lockAndGetState(instanceId);
            Assert.notNull(state, () -> "Pause failed, instance_id not found: " + instanceId);

            RunState runState = RunState.of(state);
            if (!RunState.PAUSABLE_LIST.contains(runState)) {
                return false;
            }

            // update waiting task
            List<Integer> fromStateList = Collections.singletonList(ExecuteState.WAITING.value());
            taskMapper.updateStateByInstanceId(instanceId, ExecuteState.PAUSED.value(), fromStateList, null);

            // load the alive executing tasks
            List<ExecuteParam> executingTasks = loadExecutingTasks(instanceId, Operations.PAUSE);

            if (executingTasks.isEmpty()) {
                // has non executing execute_state, update sched instance state
                List<ExecuteState> stateList = taskMapper.findMediumByInstanceId(instanceId)
                    .stream()
                    .map(e -> ExecuteState.of(e.getExecuteState()))
                    .collect(Collectors.toList());
                RunState toRunState;
                if (stateList.stream().anyMatch(ExecuteState.PAUSED::equals)) {
                    toRunState = RunState.PAUSED;
                } else if (stateList.stream().anyMatch(ExecuteState::isFailure)) {
                    toRunState = RunState.CANCELED;
                } else {
                    toRunState = RunState.FINISHED;
                }

                int row;
                if (toRunState.isTerminal()) {
                    row = instanceMapper.terminate(instanceId, toRunState.value(), Collections.singletonList(runState.value()), new Date());
                } else {
                    row = instanceMapper.updateState(instanceId, toRunState.value(), runState.value(), null);
                }
                if (row != AFFECTED_ONE_ROW) {
                    log.warn("Pause instance from {} to {} conflict", runState, toRunState);
                }
            } else {
                // dispatch and pause executing tasks
                TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(executingTasks));
            }
            return true;
        });
    }

    /**
     * Pause executing task
     *
     * @param param    the execution param
     * @param errorMsg the execution error message
     * @return {@code true} if paused successfully
     */
    @Override
    public boolean pauseExecutingTask(ExecuteParam param, String errorMsg) {
        return doTransactionInSynchronized(param.getInstanceId(), () -> {
            Integer state = instanceMapper.lockAndGetState(param.getInstanceId());
            if (!RunState.RUNNING.equals(state)) {
                log.warn("Pause executing task failed: {} | {}", param, state);
                return false;
            }

            int row = taskMapper.updateState(param.getTaskId(), ExecuteState.PAUSED.value(), ExecuteState.EXECUTING.value(), errorMsg, null);
            if (row != AFFECTED_ONE_ROW) {
                log.warn("Paused task unsuccessful.");
                return false;
            }

            boolean allPaused = taskMapper.findMediumByInstanceId(param.getInstanceId())
                .stream()
                .map(e -> ExecuteState.of(e.getExecuteState()))
                .noneMatch(ExecuteState.PAUSABLE_LIST::contains);
            if (allPaused) {
                row = instanceMapper.updateState(param.getInstanceId(), RunState.PAUSED.value(), RunState.RUNNING.value(), null);
                if (row != AFFECTED_ONE_ROW) {
                    log.error("Update sched instance to paused state conflict: {} | {}", param.getInstanceId(), param.getTaskId());
                }
            }
            return true;
        });
    }

    // ------------------------------------------------------------------manual cancel

    /**
     * Cancel task by sched instance id
     *
     * @param instanceId the instance id
     * @param operation  the operation
     * @return {@code true} if canceled successfully
     */
    @Override
    public boolean cancelInstance(long instanceId, Operations operation) {
        Assert.isTrue(operation.targetState().isFailure(), () -> "Expect cancel ops, but actual: " + operation);
        return doTransactionInSynchronized(instanceId, () -> {
            Integer state = instanceMapper.lockAndGetState(instanceId);
            Assert.notNull(state, () -> "Cancel failed, instance_id not found: " + instanceId);

            RunState runState = RunState.of(state);
            if (runState.isTerminal()) {
                return false;
            }

            // update waiting & paused state task
            taskMapper.updateStateByInstanceId(instanceId, operation.targetState().value(), EXECUTABLE_EXECUTE_STATE_LIST, new Date());

            // load the alive executing tasks
            List<ExecuteParam> executingTasks = loadExecutingTasks(instanceId, operation);

            if (executingTasks.isEmpty()) {
                // has non executing execute_state
                boolean failure = taskMapper.findMediumByInstanceId(instanceId).stream().anyMatch(e -> ExecuteState.of(e.getExecuteState()).isFailure());
                RunState toRunState = failure ? RunState.CANCELED : RunState.FINISHED;
                int row = instanceMapper.terminate(instanceId, toRunState.value(), Collections.singletonList(runState.value()), new Date());
                if (row != AFFECTED_ONE_ROW) {
                    log.warn("Pause instance from {} to {} conflict", runState, toRunState);
                }
            } else {
                // dispatch and cancel executing tasks
                TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(executingTasks));
            }
            return true;
        });
    }

    /**
     * Cancel executing task
     *
     * @param param    the execution param
     * @param toState  the target state
     * @param errorMsg the executed error message
     * @return {@code true} if canceled successfully
     */
    @Override
    public boolean cancelExecutingTask(ExecuteParam param, ExecuteState toState, String errorMsg) {
        Assert.isTrue(toState.isFailure(), () -> "Target state expect failure state, but actual: " + toState);
        return doTransactionInSynchronized(param.getInstanceId(), () -> {
            Integer state = instanceMapper.lockAndGetState(param.getInstanceId());
            if (!RunState.RUNNING.equals(state)) {
                return false;
            }

            int row = taskMapper.terminate(param.getTaskId(), toState.value(), ExecuteState.EXECUTING.value(), new Date(), errorMsg);
            if (row != AFFECTED_ONE_ROW) {
                log.warn("Canceled task unsuccessful.");
                return false;
            }

            boolean allTerminated = taskMapper.findMediumByInstanceId(param.getInstanceId())
                .stream()
                .map(e -> ExecuteState.of(e.getExecuteState()))
                .allMatch(ExecuteState::isTerminal);
            if (allTerminated) {
                row = instanceMapper.terminate(param.getInstanceId(), RunState.CANCELED.value(), Collections.singletonList(RunState.RUNNING.value()), new Date());
                if (row != AFFECTED_ONE_ROW) {
                    log.error("Update sched instance to canceled state conflict: {} | {}", param.getInstanceId(), param.getTaskId());
                }
            }
            return true;
        });
    }

    /**
     * Resume sched instance from paused state to waiting state
     *
     * @param instanceId the instanceId
     * @return {@code true} if resumed successfully
     */
    public boolean resumeInstance(long instanceId) {
        return doTransactionInSynchronized(instanceId, () -> {
            Integer state = instanceMapper.lockAndGetState(instanceId);
            Assert.notNull(state, () -> "Cancel failed, instance_id not found: " + instanceId);
            if (!RunState.PAUSED.equals(state)) {
                return false;
            }

            int row = instanceMapper.updateState(instanceId, RunState.WAITING.value(), RunState.PAUSED.value(), null);
            Assert.state(row == AFFECTED_ONE_ROW, "Resume sched instance failed.");

            row = taskMapper.updateStateByInstanceId(instanceId, ExecuteState.WAITING.value(), Collections.singletonList(ExecuteState.PAUSED.value()), null);
            Assert.state(row >= AFFECTED_ONE_ROW, "Resume sched task failed.");

            // dispatch task
            Tuple3<SchedJob, SchedInstance, List<SchedTask>> params = buildDispatchParams(instanceId, row);
            TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(params.a, params.b, params.c));
            return true;
        });
    }

    public boolean updateInstanceState(ExecuteState toState, List<SchedTask> tasks, SchedInstance instance) {
        return doTransactionInSynchronized(instance.getInstanceId(), () -> {
            if (instanceMapper.lockAndGetId(instance.getInstanceId()) == null) {
                return false;
            }
            int row = instanceMapper.updateState(instance.getInstanceId(), toState.runState().value(), instance.getRunState(), instance.getVersion());
            if (row != AFFECTED_ONE_ROW) {
                log.warn("Conflict update instance run state: {} | {}", instance, toState.runState());
                return false;
            }

            row = 0;
            for (SchedTask task : tasks) {
                row += taskMapper.updateState(task.getTaskId(), toState.value(), task.getExecuteState(), null, task.getVersion());
            }
            Assert.state(row >= AFFECTED_ONE_ROW, () -> "Conflict update state: " + toState + ", " + tasks + ", " + instance);

            // updated successfully
            return true;
        });
    }

    // ------------------------------------------------------------------private methods

    private boolean doTransactionInSynchronized(long lockKey, Supplier<Boolean> action) {
        // Also use guava:
        // private static final Interner<String> INTERNER_POOL = Interners.newWeakInterner();
        // INTERNER_POOL.intern(Long.toString(lockKey));
        synchronized (Long.toString(lockKey).intern()) {
            return Boolean.TRUE.equals(transactionTemplate.execute(status -> action.get()));
        }
    }

    private void doTransactionInSynchronized(long lockKey, Runnable action) {
        synchronized (Long.toString(lockKey).intern()) {
            transactionTemplate.executeWithoutResult(status -> action.run());
        }
    }

    private void terminatePostProcess(long instanceId, RunState runState) {
        if (runState == RunState.CANCELED) {
            retryJobIfNecessary(instanceId);
        } else if (runState == RunState.FINISHED) {
            dependJobIfNecessary(instanceId);
        } else {
            log.error("Unknown retry run state " + runState);
        }
    }

    private void retryJobIfNecessary(long instanceId) {
        SchedInstance prevInstance = instanceMapper.getByInstanceId(instanceId);
        SchedJob schedJob = jobMapper.getByJobId(prevInstance.getJobId());
        if (schedJob == null) {
            log.error("Sched job not found {}", prevInstance.getJobId());
            return;
        }

        List<SchedTask> prevTasks = taskMapper.findLargeByInstanceId(instanceId);
        RetryType retryType = RetryType.of(schedJob.getRetryType());
        if (retryType == RetryType.NONE || schedJob.getRetryCount() < 1) {
            // not retry
            return;
        }

        int retriedCount = Optional.ofNullable(prevInstance.getRetriedCount()).orElse(0);
        if (retriedCount >= schedJob.getRetryCount()) {
            // already retried maximum times
            return;
        }

        Date now = new Date();

        // 1、build sched instance
        retriedCount++;
        long triggerTime = computeRetryTriggerTime(schedJob, retriedCount, now);
        SchedInstance retryInstance = SchedInstance.create(generateId(), schedJob.getJobId(), RunType.RETRY, triggerTime, retriedCount, now);
        retryInstance.setParentInstanceId(prevInstance.obtainRootInstanceId());

        // 2、build sched tasks
        List<SchedTask> tasks;
        switch (retryType) {
            case ALL:
                try {
                    // re-split tasks
                    tasks = splitTasks(schedJob, retryInstance.getInstanceId(), now);
                } catch (Exception e) {
                    log.error("Split job error: " + schedJob + ", " + prevInstance, e);
                    return;
                }
                break;
            case FAILED:
                tasks = prevTasks.stream()
                    .filter(e -> ExecuteState.of(e.getExecuteState()).isFailure())
                    .map(e -> SchedTask.create(e.getTaskParam(), generateId(), retryInstance.getInstanceId(), now))
                    .collect(Collectors.toList());
                break;
            default:
                log.error("Job unsupported retry type {}", schedJob);
                return;
        }

        // 3、save to db
        Assert.notEmpty(tasks, "Insert list of task cannot be empty.");
        instanceMapper.insert(retryInstance);
        batchInsertTask(tasks);

        TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(schedJob, retryInstance, tasks));
    }

    /**
     * Crates dependency jbo task.
     *
     * @param instanceId the parent trace id
     */
    private void dependJobIfNecessary(long instanceId) {
        SchedInstance parentInstance = instanceMapper.getByInstanceId(instanceId);
        List<SchedDepend> schedDepends = dependMapper.findByParentJobId(parentInstance.getJobId());
        if (CollectionUtils.isEmpty(schedDepends)) {
            return;
        }

        for (SchedDepend depend : schedDepends) {
            SchedJob childJob = jobMapper.getByJobId(depend.getChildJobId());
            if (childJob == null) {
                log.error("Child sched job not found {}, {}", depend.getParentJobId(), depend.getChildJobId());
                return;
            }
            if (JobState.DISABLE.equals(childJob.getJobState())) {
                return;
            }

            try {
                Date now = new Date();
                SchedInstance instance = SchedInstance.create(generateId(), childJob.getJobId(), RunType.DEPEND, parentInstance.getTriggerTime(), 0, now);
                instance.setParentInstanceId(parentInstance.obtainRootInstanceId());
                List<SchedTask> tasks = splitTasks(childJob, instance.getInstanceId(), now);

                // save to db
                instanceMapper.insert(instance);
                batchInsertTask(tasks);

                TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(childJob, instance, tasks));
            } catch (Exception e) {
                log.error("Depend job split failed: " + childJob, e);
            }
        }
    }

    private void batchInsertTask(List<SchedTask> tasks) {
        Assert.notEmpty(tasks, "Insert list of task cannot be empty.");
        if (tasks.size() <= JobConstants.PROCESS_BATCH_SIZE) {
            int row = taskMapper.batchInsert(tasks);
            Assert.state(row == tasks.size(), () -> "Insert sched task fail: " + tasks);
        } else {
            for (List<SchedTask> list : Lists.partition(tasks, JobConstants.PROCESS_BATCH_SIZE)) {
                int row = taskMapper.batchInsert(list);
                Assert.state(row == list.size(), () -> "Insert sched task fail: " + tasks);
            }
        }
    }

    private List<ExecuteParam> loadExecutingTasks(long instanceId, Operations ops) {
        SchedInstance schedInstanceProxy = LazyLoader.of(SchedInstance.class, instanceMapper::getByInstanceId, instanceId);
        List<ExecuteParam> executingTasks = new ArrayList<>();
        taskMapper.findMediumByInstanceId(instanceId)
            .stream()
            .filter(e -> ExecuteState.EXECUTING.equals(e.getExecuteState()))
            .forEach(task -> {
                Worker worker = Worker.deserialize(task.getWorker());
                if (super.isAliveWorker(worker)) {
                    ExecuteParam param = new ExecuteParam(ops, task.getTaskId(), instanceId, schedInstanceProxy.getJobId(), 0L);
                    param.setWorker(worker);
                    executingTasks.add(param);
                } else {
                    // update dead task
                    log.info("Cancel the dead task {}", task);
                    // ExecuteState.EXECUTE_TIMEOUT.value()
                    taskMapper.updateState(task.getTaskId(), ops.targetState().value(), ExecuteState.EXECUTING.value(), null, null);
                }
            });
        return executingTasks;
    }

    private Tuple3<SchedJob, SchedInstance, List<SchedTask>> buildDispatchParams(long instanceId, int expectTaskSize) {
        SchedInstance instance = instanceMapper.getByInstanceId(instanceId);
        SchedJob job = jobMapper.getByJobId(instance.getJobId());
        List<SchedTask> waitingTasks = taskMapper.findLargeByInstanceId(instanceId)
            .stream()
            .filter(e -> ExecuteState.WAITING.equals(e.getExecuteState()))
            .collect(Collectors.toList());
        Assert.isTrue(
            waitingTasks.size() == expectTaskSize,
            () -> "Dispatching tasks size inconsistent, expect=" + expectTaskSize + ", actual=" + waitingTasks.size()
        );
        return Tuple3.of(job, instance, waitingTasks);
    }

    private void parseTriggerConfig(SchedJob job, Date date) {
        TriggerType triggerType = TriggerType.of(job.getTriggerType());
        Assert.isTrue(
            triggerType.isValid(job.getTriggerValue()),
            () -> "Invalid trigger value: " + job.getTriggerType() + ", " + job.getTriggerValue()
        );

        if (triggerType == TriggerType.DEPEND) {
            List<Long> parentJobIds = Arrays.stream(job.getTriggerValue().split(Str.COMMA))
                .filter(StringUtils::isNotBlank)
                .map(e -> Long.parseLong(e.trim()))
                .distinct()
                .collect(Collectors.toList());
            Assert.notEmpty(parentJobIds, () -> "Invalid dependency parent job id config: " + job.getTriggerValue());

            Map<Long, SchedJob> parentJobMap = jobMapper.findByJobIds(parentJobIds)
                .stream()
                .collect(Collectors.toMap(SchedJob::getJobId, Function.identity()));
            for (Long parentJobId : parentJobIds) {
                SchedJob parentJob = parentJobMap.get(parentJobId);
                Assert.notNull(parentJob, () -> "Parent job id not found: " + parentJobId);
                Assert.isTrue(
                    job.getJobGroup().equals(parentJob.getJobGroup()),
                    () -> "Parent job '" + parentJob.getJobId() + "' group '" + parentJob.getJobGroup() + "' different '" + job.getJobGroup() + "'"
                );
            }
            dependMapper.insertBatch(parentJobIds.stream().map(e -> new SchedDepend(e, job.getJobId())).collect(Collectors.toList()));
            job.setTriggerValue(Joiner.on(Str.COMMA).join(parentJobIds));
            job.setNextTriggerTime(null);
        } else {
            Date nextTriggerTime = triggerType.computeNextFireTime(job.getTriggerValue(), date);
            Assert.notNull(nextTriggerTime, () -> "Has not next trigger time " + job.getTriggerValue());
            job.setNextTriggerTime(nextTriggerTime.getTime());
        }
    }

    /**
     * Returns the retry trigger time
     *
     * @param job       the SchedJob
     * @param failCount the failure times
     * @param current   the current date time
     * @return retry trigger time milliseconds
     */
    private static long computeRetryTriggerTime(SchedJob job, int failCount, Date current) {
        Assert.isTrue(!RetryType.NONE.equals(job.getRetryType()), () -> "Sched job '" + job.getJobId() + "' retry type is NONE.");
        Assert.isTrue(job.getRetryCount() > 0, () -> "Sched job '" + job.getJobId() + "' retry count must greater than 0, but actual " + job.getRetryCount());
        Assert.isTrue(failCount <= job.getRetryCount(), () -> "Sched job '" + job.getJobId() + "' retried " + failCount + " exceed " + job.getRetryCount() + " limit.");
        // exponential backoff
        return current.getTime() + (long) job.getRetryInterval() * IntMath.pow(failCount, 2);
    }

}
