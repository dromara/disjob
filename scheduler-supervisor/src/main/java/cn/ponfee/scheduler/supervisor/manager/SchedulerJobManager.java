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
import cn.ponfee.scheduler.common.base.tuple.Tuple2;
import cn.ponfee.scheduler.common.base.tuple.Tuple3;
import cn.ponfee.scheduler.common.spring.RpcController;
import cn.ponfee.scheduler.common.spring.TransactionUtils;
import cn.ponfee.scheduler.common.util.Collects;
import cn.ponfee.scheduler.common.util.ObjectUtils;
import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.SupervisorService;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.enums.*;
import cn.ponfee.scheduler.core.exception.JobException;
import cn.ponfee.scheduler.core.model.SchedDepend;
import cn.ponfee.scheduler.core.model.SchedInstance;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.core.param.ExecuteTaskParam;
import cn.ponfee.scheduler.core.param.StartTaskParam;
import cn.ponfee.scheduler.core.param.TaskWorkerParam;
import cn.ponfee.scheduler.core.param.TerminateTaskParam;
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
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.ponfee.scheduler.supervisor.base.AbstractDataSourceConfig.TX_MANAGER_NAME_SUFFIX;
import static cn.ponfee.scheduler.supervisor.base.AbstractDataSourceConfig.TX_TEMPLATE_NAME_SUFFIX;
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

    private static final String TX_MANAGER_NAME = DB_NAME + TX_MANAGER_NAME_SUFFIX;
    private static final int AFFECTED_ONE_ROW = 1;

    private static final List<Integer> RUN_STATE_CANCELABLE = Collects.convert(RunState.CANCELABLE_LIST, RunState::value);

    private static final List<Integer> EXECUTE_STATE_EXECUTABLE = Collects.convert(ExecuteState.EXECUTABLE_LIST, ExecuteState::value);
    private static final List<Integer> EXECUTE_STATE_PAUSED = Collections.singletonList(ExecuteState.PAUSED.value());
    private static final List<Integer> EXECUTE_STATE_WAITING = Collections.singletonList(ExecuteState.WAITING.value());
    private static final List<Integer> EXECUTE_STATE_PAUSABLE = Collects.convert(ExecuteState.PAUSABLE_LIST, ExecuteState::value);

    private final TransactionTemplate transactionTemplate;
    private final SchedJobMapper jobMapper;
    private final SchedInstanceMapper instanceMapper;
    private final SchedTaskMapper taskMapper;
    private final SchedDependMapper dependMapper;

    public SchedulerJobManager(IdGenerator idGenerator,
                               SupervisorRegistry discoveryWorker,
                               TaskDispatcher taskDispatcher,
                               WorkerServiceClient workerServiceClient,
                               @Qualifier(DB_NAME + TX_TEMPLATE_NAME_SUFFIX) TransactionTemplate transactionTemplate,
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
     * @param size               the query data size
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

    // ------------------------------------------------------------------database single operation without transactional

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

    public boolean stopJob(SchedJob job) {
        return AFFECTED_ONE_ROW == jobMapper.stop(job);
    }

    public boolean updateNextTriggerTime(SchedJob job) {
        return jobMapper.updateNextTriggerTime(job) == AFFECTED_ONE_ROW;
    }

    public boolean updateNextScanTime(long jobId, Date nextScanTime, int version) {
        return jobMapper.updateNextScanTime(jobId, nextScanTime, version) == AFFECTED_ONE_ROW;
    }

    @Override
    protected boolean cancelWaitingTask(long taskId) {
        return taskMapper.terminate(taskId, ExecuteState.WAITING_CANCELED.value(), ExecuteState.WAITING.value(), null, null) == AFFECTED_ONE_ROW;
    }

    // ------------------------------------------------------------------operation within transactional

    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    public void addJob(SchedJob job) {
        job.verifyBeforeAdd();

        super.verifyJob(job);
        job.checkAndDefaultSetting();

        job.setJobId(generateId());
        Date now = new Date();
        parseTriggerConfig(job, now);

        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        jobMapper.insert(job);
    }

    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    public void updateJob(SchedJob job) {
        job.verifyBeforeUpdate();

        if (StringUtils.isEmpty(job.getJobHandler())) {
            Assert.isTrue(StringUtils.isEmpty(job.getJobParam()), "Job param must be null if not set job handler.");
        } else {
            super.verifyJob(job);
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
        Assert.state(jobMapper.updateByJobId(job) == AFFECTED_ONE_ROW, "Update sched job fail or conflict.");
    }

    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    public void deleteJob(long jobId) {
        Assert.isTrue(jobMapper.deleteByJobId(jobId) == AFFECTED_ONE_ROW, "Delete sched job fail or conflict.");
        dependMapper.deleteByParentJobId(jobId);
        dependMapper.deleteByChildJobId(jobId);
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

    public void forceChangeState(long instanceId, int targetExecuteState) {
        ExecuteState toExecuteState = ExecuteState.of(targetExecuteState);
        RunState toRunState = toExecuteState.runState();
        Assert.isTrue(toExecuteState != ExecuteState.EXECUTING, "Cannot force update state to EXECUTING");
        doTransactionInSynchronized(instanceId, () -> {
            Assert.notNull(instanceMapper.lockAndGetId(instanceId), () -> "Sched instance not found: " + instanceId);

            int row1 = instanceMapper.forceChangeState(instanceId, toRunState.value());
            int row2 = taskMapper.forceChangeState(instanceId, toExecuteState.value());
            if (row1 == 0 && row2 == 0) {
                throw new IllegalStateException("Force update instance state failed: " + instanceId);
            }

            if (toExecuteState == ExecuteState.WAITING) {
                Tuple3<SchedJob, SchedInstance, List<SchedTask>> params = buildDispatchParams(instanceId, row2);
                TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(params.a, params.b, params.c));
            }

            log.info("Force change state success {} | {}", instanceId, toExecuteState);
        });
    }

    /**
     * Set or clear task worker
     *
     * @param params the list of update task worker params
     */
    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    @Override
    public void updateTaskWorker(List<TaskWorkerParam> params) {
        if (CollectionUtils.isEmpty(params)) {
            return;
        }
        // Sort for prevent sql deadlock: Deadlock found when trying to get lock; try restarting transaction
        params.sort(Comparator.comparing(TaskWorkerParam::getTaskId));
        if (params.size() <= JobConstants.PROCESS_BATCH_SIZE) {
            taskMapper.batchUpdateWorker(params);
        } else {
            Lists.partition(params, JobConstants.PROCESS_BATCH_SIZE).forEach(taskMapper::batchUpdateWorker);
        }
    }

    /**
     * Starts the task
     *
     * @param param the start task param
     * @return {@code true} if start successfully
     */
    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    @Override
    public boolean startTask(StartTaskParam param) {
        Integer state = instanceMapper.getStateByInstanceId(param.getInstanceId());
        Assert.state(state != null, () -> "Sched instance not found: " + param);
        RunState runState = RunState.of(state);
        // sched_instance.run_state must in (WAITING, RUNNING)
        Assert.state(RunState.PAUSABLE_LIST.contains(runState), () -> "Start instance failed: " + param + ", " + runState);

        Date now = new Date();
        // start sched instance(also possibly started by other task)
        int instanceRow = instanceMapper.start(param.getInstanceId(), now);

        // start sched task
        int taskRow = taskMapper.start(param.getTaskId(), param.getWorker(), now);

        if (instanceRow == 0 && taskRow == 0) {
            // conflict: the task executed by other executor
            return false;
        }

        Assert.state(taskRow == AFFECTED_ONE_ROW, () -> "Start task failed: " + param);
        return true;
    }

    // ------------------------------------------------------------------terminate task & instance

    /**
     * Terminate task
     *
     * @param param the terminal task param
     * @return {@code true} if terminated task successful
     */
    @Override
    public boolean terminateTask(TerminateTaskParam param) {
        ExecuteState toState = param.getToState();
        Assert.isTrue(!ExecuteState.PAUSABLE_LIST.contains(toState), () -> "Stop executing invalid to state " + toState);
        return doTransactionInSynchronized(param.getInstanceId(), () -> {
            Integer state = instanceMapper.lockAndGetState(param.getInstanceId());
            Assert.notNull(state, () -> "Terminate executing task failed, instance not found: " + param.getInstanceId());
            if (RunState.of(state).isTerminal()) {
                // already terminated
                return false;
            }

            Date executeEndTime = toState.isTerminal() ? new Date() : null;
            int row = taskMapper.terminate(param.getTaskId(), toState.value(), ExecuteState.EXECUTING.value(), executeEndTime, param.getErrorMsg());
            if (row != AFFECTED_ONE_ROW) {
                // usual is worker invoke http timeout, then retry
                log.warn("Conflict terminate executing task: {} | {}", param.getTaskId(), toState);
                return false;
            }

            Tuple2<RunState, Date> tuple = obtainRunState(taskMapper.findMediumByInstanceId(param.getInstanceId()));
            if (tuple != null) {
                // the last executing task of this sched instance
                row = instanceMapper.terminate(param.getInstanceId(), tuple.a.value(), RUN_STATE_CANCELABLE, tuple.b);
                if (row == AFFECTED_ONE_ROW && param.getOperation() == Operations.TRIGGER) {
                    afterTerminateTask(param.getInstanceId(), tuple.a);
                }
            }

            return true;
        });
    }

    /**
     * Purge the zombie instance which maybe dead
     *
     * @param instanceId the instance id
     * @return {@code true} if purged successfully
     */
    public boolean purgeInstance(long instanceId) {
        return doTransactionInSynchronized(instanceId, () -> {
            Integer state = instanceMapper.lockAndGetState(instanceId);
            Assert.notNull(state, () -> "Purge instance not found: " + instanceId);
            // instance run state must in (10, 20)
            if (!RunState.PAUSABLE_LIST.contains(RunState.of(state))) {
                return false;
            }

            // task execute state must not 10
            List<SchedTask> tasks = taskMapper.findMediumByInstanceId(instanceId);
            if (tasks.stream().anyMatch(e -> ExecuteState.WAITING.equals(e.getExecuteState()))) {
                log.warn("Purge instance failed, has waiting task: {}", tasks);
                return false;
            }

            // if task execute state is 20, must not is alive
            if (hasAliveExecuting(tasks)) {
                log.warn("Purge instance failed, has alive executing task: {}", tasks);
                return false;
            }

            Tuple2<RunState, Date> tuple = ObjectUtils.defaultIfNull(obtainRunState(tasks), () -> Tuple2.of(RunState.CANCELED, new Date()));
            if (instanceMapper.terminate(instanceId, tuple.a.value(), RUN_STATE_CANCELABLE, tuple.b) != AFFECTED_ONE_ROW) {
                return false;
            }

            tasks.stream()
                .filter(e -> EXECUTE_STATE_PAUSABLE.contains(e.getExecuteState()))
                .forEach(e -> taskMapper.terminate(e.getTaskId(), ExecuteState.EXECUTE_TIMEOUT.value(), e.getExecuteState(), new Date(), null));

            afterTerminateTask(instanceId, tuple.a);

            log.warn("Purge instance {} to state {}", instanceId, tuple.a);
            return true;
        });
    }

    /**
     * Pause instance
     *
     * @param instanceId the instance id
     * @return {@code true} if paused successfully
     */
    @Override
    public boolean pauseInstance(long instanceId) {
        return doTransactionInSynchronized(instanceId, () -> {
            SchedInstance instance = instanceMapper.lockAndGet(instanceId);
            Assert.notNull(instance, () -> "Pause instance not found: " + instanceId);
            RunState runState = RunState.of(instance.getRunState());
            if (!RunState.PAUSABLE_LIST.contains(runState)) {
                return false;
            }

            Operations ops = Operations.PAUSE;

            // 1、update: (WAITING) -> (PAUSE)
            taskMapper.updateStateByInstanceId(instanceId, ops.toState().value(), EXECUTE_STATE_WAITING, null);

            // 2、load the alive executing tasks
            List<ExecuteTaskParam> executingTasks = loadExecutingTasks(instance, ops);
            if (executingTasks.isEmpty()) {
                // 2.1、has non executing task, update sched instance state
                Tuple2<RunState, Date> tuple = obtainRunState(taskMapper.findMediumByInstanceId(instanceId));
                // must be paused or terminate
                Assert.notNull(tuple, () -> "Pause instance failed: " + instanceId);
                if (instanceMapper.terminate(instanceId, tuple.a.value(), RUN_STATE_CANCELABLE, tuple.b) != AFFECTED_ONE_ROW) {
                    log.warn("Pause instance from {} to {} conflict", runState, tuple.a);
                }
            } else {
                // 2.2、has alive executing tasks: dispatch and pause executing tasks
                TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(executingTasks));
            }

            return true;
        });
    }

    /**
     * Cancel instance
     *
     * @param instanceId the instance id
     * @param ops        the operation
     * @return {@code true} if canceled successfully
     */
    @Override
    public boolean cancelInstance(long instanceId, Operations ops) {
        Assert.isTrue(ops.toState().isFailure(), () -> "Cancel instance operation invalid: " + ops);
        return doTransactionInSynchronized(instanceId, () -> {
            SchedInstance instance = instanceMapper.lockAndGet(instanceId);
            Assert.notNull(instance, () -> "Cancel instance not found: " + instanceId);
            RunState runState = RunState.of(instance.getRunState());
            if (runState.isTerminal()) {
                return false;
            }

            // 1、update: (WAITING or PAUSED) -> (CANCELED)
            taskMapper.updateStateByInstanceId(instanceId, ops.toState().value(), EXECUTE_STATE_EXECUTABLE, new Date());

            // 2、load the alive executing tasks
            List<ExecuteTaskParam> executingTasks = loadExecutingTasks(instance, ops);
            if (executingTasks.isEmpty()) {
                // 2.1、has non executing execute_state
                Tuple2<RunState, Date> tuple = obtainRunState(taskMapper.findMediumByInstanceId(instanceId));
                Assert.notNull(tuple, () -> "Cancel instance failed: " + instanceId);
                // if all task paused, should update to canceled state
                if (tuple.a == RunState.PAUSED) {
                    tuple = Tuple2.of(RunState.CANCELED, new Date());
                }
                if (instanceMapper.terminate(instanceId, tuple.a.value(), RUN_STATE_CANCELABLE, tuple.b) != AFFECTED_ONE_ROW) {
                    log.warn("Cancel instance from {} to {} conflict", runState, tuple.a);
                }
            } else {
                // 2.2、dispatch and cancel executing tasks
                TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(executingTasks));
            }

            return true;
        });
    }

    /**
     * Resume the instance from paused to waiting state
     *
     * @param instanceId the instance id
     * @return {@code true} if resumed successfully
     */
    public boolean resumeInstance(long instanceId) {
        return doTransactionInSynchronized(instanceId, () -> {
            Integer state = instanceMapper.lockAndGetState(instanceId);
            Assert.notNull(state, () -> "Cancel failed, instance_id not found: " + instanceId);
            if (!RunState.PAUSED.equals(state)) {
                return false;
            }

            int row = instanceMapper.updateState(instanceId, RunState.WAITING.value(), RunState.PAUSED.value());
            Assert.state(row == AFFECTED_ONE_ROW, "Resume sched instance failed.");

            row = taskMapper.updateStateByInstanceId(instanceId, ExecuteState.WAITING.value(), EXECUTE_STATE_PAUSED, null);
            Assert.state(row >= AFFECTED_ONE_ROW, "Resume sched task failed.");

            // dispatch task
            Tuple3<SchedJob, SchedInstance, List<SchedTask>> params = buildDispatchParams(instanceId, row);
            TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(params.a, params.b, params.c));
            return true;
        });
    }

    // ------------------------------------------------------------------private methods

    private boolean doTransactionInSynchronized(long lockKey, BooleanSupplier action) {
        // Also use guava:
        // private static final Interner<String> INTERNER_POOL = Interners.newWeakInterner();
        // INTERNER_POOL.intern(Long.toString(lockKey));
        synchronized (Long.toString(lockKey).intern()) {
            return Boolean.TRUE.equals(transactionTemplate.execute(status -> action.getAsBoolean()));
        }
    }

    private void doTransactionInSynchronized(long lockKey, Runnable action) {
        synchronized (Long.toString(lockKey).intern()) {
            transactionTemplate.executeWithoutResult(status -> action.run());
        }
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
        return states.stream().anyMatch(ExecuteState.PAUSABLE_LIST::contains) ? null : Tuple2.of(RunState.PAUSED, null);
    }

    private void afterTerminateTask(long instanceId, RunState runState) {
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
                    .filter(e -> !JobType.BROADCAST.equals(schedJob.getJobType()) || super.isAliveWorker(e.getWorker()))
                    .map(e -> {
                        SchedTask task = SchedTask.create(e.getTaskParam(), generateId(), retryInstance.getInstanceId(), now);
                        task.setWorker(e.getWorker());
                        return task;
                    })
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
                log.error("Child sched job not found: {} | {}", depend.getParentJobId(), depend.getChildJobId());
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
            List<List<SchedTask>> partition = Lists.partition(tasks, JobConstants.PROCESS_BATCH_SIZE);
            for (List<SchedTask> list : partition) {
                int row = taskMapper.batchInsert(list);
                Assert.state(row == list.size(), () -> "Insert sched task fail: " + tasks);
            }
        }
    }

    private List<ExecuteTaskParam> loadExecutingTasks(SchedInstance instance, Operations ops) {
        List<ExecuteTaskParam> executingTasks = new ArrayList<>();
        SchedJob jobProxy = LazyLoader.of(SchedJob.class, jobMapper::getByJobId, instance.getJobId());
        taskMapper.findMediumByInstanceId(instance.getInstanceId())
            .stream()
            .filter(e -> ExecuteState.EXECUTING.equals(e.getExecuteState()))
            .forEach(task -> {
                Worker worker = Worker.deserialize(task.getWorker());
                if (super.isAliveWorker(worker)) {
                    executingTasks.add(new ExecuteTaskParam(ops, task.getTaskId(), instance.getInstanceId(), instance.getJobId(), JobType.of(jobProxy.getJobType()), 0L, worker));
                } else {
                    // update dead task
                    Date executeEndTime = ops.toState().isTerminal() ? new Date() : null;
                    int row = taskMapper.terminate(task.getTaskId(), ops.toState().value(), ExecuteState.EXECUTING.value(), executeEndTime, null);
                    if (row != AFFECTED_ONE_ROW) {
                        log.warn("Cancel the dead task failed: {}", task);
                        executingTasks.add(new ExecuteTaskParam(ops, task.getTaskId(), instance.getInstanceId(), instance.getJobId(), JobType.of(jobProxy.getJobType()), 0L, worker));
                    } else {
                        log.info("Cancel the dead task success: {}", task);
                    }
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
