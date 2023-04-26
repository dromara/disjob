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
import cn.ponfee.scheduler.common.graph.DAGEdge;
import cn.ponfee.scheduler.common.graph.DAGNode;
import cn.ponfee.scheduler.common.spring.RpcController;
import cn.ponfee.scheduler.common.spring.TransactionUtils;
import cn.ponfee.scheduler.common.tuple.Tuple2;
import cn.ponfee.scheduler.common.tuple.Tuple3;
import cn.ponfee.scheduler.common.util.Collects;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.common.util.ObjectUtils;
import cn.ponfee.scheduler.core.base.SupervisorService;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.enums.*;
import cn.ponfee.scheduler.core.exception.JobException;
import cn.ponfee.scheduler.core.graph.WorkflowGraph;
import cn.ponfee.scheduler.core.model.*;
import cn.ponfee.scheduler.core.param.*;
import cn.ponfee.scheduler.dispatch.TaskDispatcher;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import cn.ponfee.scheduler.supervisor.base.WorkerServiceClient;
import cn.ponfee.scheduler.supervisor.dao.mapper.*;
import cn.ponfee.scheduler.supervisor.instance.NormalInstanceCreator;
import cn.ponfee.scheduler.supervisor.instance.TriggerInstance;
import cn.ponfee.scheduler.supervisor.instance.TriggerInstanceCreator;
import cn.ponfee.scheduler.supervisor.instance.WorkflowInstanceCreator;
import cn.ponfee.scheduler.supervisor.param.SplitJobParam;
import com.google.common.base.Joiner;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.math.IntMath;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.ponfee.scheduler.core.base.JobConstants.PROCESS_BATCH_SIZE;
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
    private static final Interner<Long> INTERNER_POOL = Interners.newWeakInterner();

    private static final List<Integer> RUN_STATE_CANCELABLE = Collects.convert(RunState.CANCELABLE_LIST, RunState::value);
    private static final List<Integer> RUN_STATE_PAUSABLE = Collects.convert(RunState.PAUSABLE_LIST, RunState::value);
    private static final List<Integer> RUN_STATE_WAITING = Collections.singletonList(RunState.WAITING.value());
    private static final List<Integer> RUN_STATE_RUNNING = Collections.singletonList(RunState.RUNNING.value());

    private static final List<Integer> EXECUTE_STATE_EXECUTABLE = Collects.convert(ExecuteState.EXECUTABLE_LIST, ExecuteState::value);
    private static final List<Integer> EXECUTE_STATE_PAUSED = Collections.singletonList(ExecuteState.PAUSED.value());
    private static final List<Integer> EXECUTE_STATE_WAITING = Collections.singletonList(ExecuteState.WAITING.value());
    private static final List<Integer> EXECUTE_STATE_PAUSABLE = Collects.convert(ExecuteState.PAUSABLE_LIST, ExecuteState::value);

    private final TransactionTemplate transactionTemplate;
    private final SchedJobMapper jobMapper;
    private final SchedInstanceMapper instanceMapper;
    private final SchedTaskMapper taskMapper;
    private final SchedDependMapper dependMapper;
    private final SchedWorkflowMapper workflowMapper;

    public SchedulerJobManager(IdGenerator idGenerator,
                               SupervisorRegistry discoveryWorker,
                               TaskDispatcher taskDispatcher,
                               WorkerServiceClient workerServiceClient,
                               @Qualifier(DB_NAME + TX_TEMPLATE_NAME_SUFFIX) TransactionTemplate transactionTemplate,
                               SchedJobMapper jobMapper,
                               SchedInstanceMapper instanceMapper,
                               SchedTaskMapper taskMapper,
                               SchedDependMapper dependMapper,
                               SchedWorkflowMapper workflowMapper) {
        super(idGenerator, discoveryWorker, taskDispatcher, workerServiceClient);
        this.transactionTemplate = transactionTemplate;
        this.jobMapper = jobMapper;
        this.instanceMapper = instanceMapper;
        this.taskMapper = taskMapper;
        this.dependMapper = dependMapper;
        this.workflowMapper = workflowMapper;
    }

    // ------------------------------------------------------------------database query

    public SchedJob getJob(long jobId) {
        return jobMapper.getByJobId(jobId);
    }

    public SchedInstance getInstance(long instanceId) {
        return instanceMapper.getByInstanceId(instanceId);
    }

    public Long getWorkflowInstanceId(long instanceId) {
        return instanceMapper.getWorkflowInstanceId(instanceId);
    }

    public List<SchedTask> findMediumTaskByInstanceId(long instanceId) {
        return taskMapper.findMediumByInstanceId(instanceId);
    }

    public List<SchedTask> findLargeTaskByInstanceId(long instanceId) {
        return taskMapper.findLargeByInstanceId(instanceId);
    }

    @Override
    public SchedTask getTask(long taskId) {
        return taskMapper.getByTaskId(taskId);
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

    public List<SchedInstance> findUnterminatedRetry(long rootInstanceId) {
        return instanceMapper.findUnterminatedRetry(rootInstanceId);
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
        return jobMapper.stop(job) == AFFECTED_ONE_ROW;
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
            Assert.hasText(job.getJobParam(), "Job param must be null if not set job handler.");
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

        TriggerInstanceCreator creator = TriggerInstanceCreator.of(job.getJobType(), this);
        TriggerInstance tInstance = creator.create(job, RunType.MANUAL, System.currentTimeMillis());
        createInstance(tInstance);
        TransactionUtils.doAfterTransactionCommit(() -> creator.dispatch(job, tInstance));
    }

    /**
     * Update sched job, save sched instance and tasks.
     *
     * @param job       the job
     * @param tInstance the trigger instance
     * @return {@code true} if operated success
     */
    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    public boolean createInstance(SchedJob job, TriggerInstance tInstance) {
        int row = jobMapper.updateNextTriggerTime(job);
        if (row == 0) {
            // conflict operation, need not process
            return false;
        }

        createInstance(tInstance);
        return true;
    }

    public void deleteInstance(long instanceId, Long workflowInstanceId) {
        doTransactionLockInSynchronized(instanceId, workflowInstanceId, instance -> {
            Assert.notNull(instance, () -> "Sched instance not found: " + instanceId);

            RunState runState = RunState.of(instance.getRunState());
            Assert.isTrue(runState.isTerminal(), () -> "Cannot delete unterminated sched instance: " + instanceId + ", run state=" + runState);

            int row = instanceMapper.deleteByInstanceId(instanceId);
            Assert.isTrue(row == AFFECTED_ONE_ROW, () -> "Delete sched instance conflict: " + instanceId);

            taskMapper.deleteByInstanceId(instanceId);
        });
    }

    public void forceChangeState(long instanceId, Long workflowInstanceId, int targetExecuteState) {
        ExecuteState toExecuteState = ExecuteState.of(targetExecuteState);
        RunState toRunState = toExecuteState.runState();
        Assert.isTrue(toExecuteState != ExecuteState.EXECUTING, "Cannot force update state to EXECUTING");
        doTransactionLockInSynchronized(instanceId, workflowInstanceId, instance -> {
            Assert.notNull(instance, () -> "Sched instance not found: " + instanceId);

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
        Collects.batchProcess(params, taskMapper::batchUpdateWorker, PROCESS_BATCH_SIZE);
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
        SchedInstance instance = instanceMapper.getByInstanceId(param.getInstanceId());
        Assert.notNull(instance, () -> "Sched instance not found: " + param);
        // sched_instance.run_state must in (WAITING, RUNNING)
        Integer state = instance.getRunState();
        Assert.state(RUN_STATE_PAUSABLE.contains(state), () -> "Start instance failed: " + param + ", " + state);

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
        return doTransactionLockInSynchronized(param.getInstanceId(), param.getWorkflowInstanceId(), instance -> {
            Assert.notNull(instance, () -> "Terminate executing task failed, instance not found: " + param.getInstanceId());
            if (RunState.of(instance.getRunState()).isTerminal()) {
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
                    instance.setRunState(tuple.a.value());
                    afterTerminateTask(instance);
                }
            }

            return true;
        });
    }

    /**
     * Purge the zombie instance which maybe dead
     *
     * @param st the sched instance
     * @return {@code true} if purged successfully
     */
    public boolean purgeInstance(SchedInstance st) {
        Long instanceId = st.getInstanceId(), workflowInstanceId = st.getWorkflowInstanceId();
        return doTransactionLockInSynchronized(instanceId, workflowInstanceId, instance -> {
            Assert.notNull(instance, () -> "Purge instance not found: " + instanceId);
            // instance run state must in (10, 20)
            if (!RUN_STATE_PAUSABLE.contains(instance.getRunState())) {
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

            instance.setRunState(tuple.a.value());
            afterTerminateTask(instance);

            log.warn("Purge instance {} to state {}", instanceId, tuple.a);
            return true;
        });
    }

    /**
     * Pause instance
     *
     * @param instanceId         the instance id
     * @param workflowInstanceId the workflow instance id
     * @return {@code true} if paused successfully
     */
    @Override
    public boolean pauseInstance(long instanceId, Long workflowInstanceId) {
        return doTransactionLockInSynchronized(instanceId, workflowInstanceId, instance -> {
            Assert.notNull(instance, () -> "Pause instance not found: " + instanceId);
            if (!RUN_STATE_PAUSABLE.contains(instance.getRunState())) {
                return false;
            }

            Operations ops = Operations.PAUSE;

            // 1、update: (WAITING) -> (PAUSE)
            taskMapper.updateStateByInstanceId(instanceId, ops.toState().value(), EXECUTE_STATE_WAITING, null);

            // 2、load the alive executing tasks
            List<ExecuteTaskParam> executingTasks = loadExecutingTasks(instance, ops);
            if (executingTasks.isEmpty()) {
                // has non executing task, update sched instance state
                Tuple2<RunState, Date> tuple = obtainRunState(taskMapper.findMediumByInstanceId(instanceId));
                // must be paused or terminate
                Assert.notNull(tuple, () -> "Pause instance failed: " + instanceId);
                if (instanceMapper.terminate(instanceId, tuple.a.value(), RUN_STATE_CANCELABLE, tuple.b) != AFFECTED_ONE_ROW) {
                    log.warn("Pause instance from {} to {} conflict", RunState.of(instance.getRunState()), tuple.a);
                }
            } else {
                // has alive executing tasks: dispatch and pause executing tasks
                TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(executingTasks));
            }

            return true;
        });
    }

    /**
     * Cancel instance
     *
     * @param instanceId         the instance id
     * @param workflowInstanceId the workflow instance id
     * @param ops                the operation
     * @return {@code true} if canceled successfully
     */
    @Override
    public boolean cancelInstance(long instanceId, Long workflowInstanceId, Operations ops) {
        Assert.isTrue(ops.toState().isFailure(), () -> "Cancel instance operation invalid: " + ops);
        return doTransactionLockInSynchronized(instanceId, workflowInstanceId, instance -> {
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
                // has non executing execute_state
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
                // dispatch and cancel executing tasks
                TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(executingTasks));
            }

            return true;
        });
    }

    /**
     * Resume the instance from paused to waiting state
     *
     * @param instanceId         the instance id
     * @param workflowInstanceId the workflow instance id
     * @return {@code true} if resumed successfully
     */
    public boolean resumeInstance(long instanceId, Long workflowInstanceId) {
        return doTransactionLockInSynchronized(instanceId, workflowInstanceId, instance -> {
            Assert.notNull(instance, () -> "Cancel failed, instance_id not found: " + instanceId);
            if (!RunState.PAUSED.equals(instance.getRunState())) {
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

    private void createInstance(TriggerInstance tInstance) {
        instanceMapper.insert(tInstance.getInstance());

        if (tInstance instanceof NormalInstanceCreator.NormalInstance) {
            NormalInstanceCreator.NormalInstance creator = (NormalInstanceCreator.NormalInstance) tInstance;
            Collects.batchProcess(creator.getTasks(), taskMapper::batchInsert, PROCESS_BATCH_SIZE);
        } else if (tInstance instanceof WorkflowInstanceCreator.WorkflowInstance) {
            WorkflowInstanceCreator.WorkflowInstance creator = (WorkflowInstanceCreator.WorkflowInstance) tInstance;
            Collects.batchProcess(creator.getWorkflows(), workflowMapper::batchInsert, PROCESS_BATCH_SIZE);
            for (Tuple2<SchedInstance, List<SchedTask>> sub : creator.getSubInstances()) {
                instanceMapper.insert(sub.a);
                Collects.batchProcess(sub.b, taskMapper::batchInsert, PROCESS_BATCH_SIZE);
            }
        } else {
            throw new UnsupportedOperationException("Unknown instance creator type: " + tInstance.getClass());
        }
    }

    private void doTransactionLockInSynchronized(long instanceId, Long workflowInstanceId, Consumer<SchedInstance> action) {
        doTransactionLockInSynchronized(instanceId, workflowInstanceId, instance -> {
                action.accept(instance);
                return Boolean.TRUE;
            }
        );
    }

    private boolean doTransactionLockInSynchronized(long instanceId, Long workflowInstanceId, Function<SchedInstance, Boolean> action) {
        // Long.toString(lockKey).intern()
        Long lockKey = workflowInstanceId == null ? instanceId : workflowInstanceId;
        synchronized (INTERNER_POOL.intern(lockKey)) {
            Boolean result = transactionTemplate.execute(status -> {
                final SchedInstance instance;
                if (workflowInstanceId == null) {
                    instance = instanceMapper.lock(instanceId);
                } else {
                    SchedInstance inst = instanceMapper.lock(workflowInstanceId);
                    instance = (instanceId == workflowInstanceId) ? inst : instanceMapper.getByInstanceId(instanceId);
                }
                return action.apply(instance);
            });
            return Boolean.TRUE.equals(result);
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

    private void afterTerminateTask(SchedInstance instance) {
        RunState runState = RunState.of(instance.getRunState());
        if (runState == RunState.CANCELED) {
            retryJob(instance);
        } else if (runState == RunState.FINISHED) {
            if (instance.isWorkflowNode()) {
                processWorkflow(instance);
            } else {
                dependJob(instance);
            }
        } else {
            log.error("Unknown terminate run state " + runState);
        }
    }

    private void processWorkflow(SchedInstance subInstance) {
        if (!subInstance.isWorkflowNode()) {
            return;
        }

        RunState runState = RunState.of(subInstance.getRunState());
        Assert.hasText(subInstance.getAttach(), () -> "Workflow instance attach cannot blank: " + subInstance);
        InstanceAttach attach = Jsons.fromJson(subInstance.getAttach(), InstanceAttach.class);
        Long workflowInstanceId = subInstance.getWorkflowInstanceId();
        Assert.isTrue(TransactionSynchronizationManager.isActualTransactionActive(), "Workflow instance must be in transaction.");
        SchedInstance workflowInstance = instanceMapper.getByInstanceId(workflowInstanceId);

        int row = workflowMapper.update(workflowInstanceId, attach.getCurNode(), runState.value(), null, RUN_STATE_CANCELABLE, null);
        if (row < AFFECTED_ONE_ROW) {
            log.warn("Update workflow node conflict: {} | {}", subInstance, runState);
            return;
        }

        if (runState == RunState.CANCELED) {
            workflowMapper.cancelWorkflow(workflowInstanceId);
        }

        WorkflowGraph graph = new WorkflowGraph(workflowMapper.findByWorkflowInstanceId(workflowInstanceId));

        // if end node is not terminal state, then process the end node run state
        if (graph.anyMatch(e -> e.getKey().getTarget().isEnd() && !e.getValue().isTerminal())) {
            Map<DAGEdge, SchedWorkflow> ends = graph.predecessors(DAGNode.END);
            if (ends.values().stream().allMatch(SchedWorkflow::isTerminal)) {
                RunState endState = ends.values().stream().anyMatch(SchedWorkflow::isFailure) ? RunState.CANCELED : RunState.FINISHED;
                row = workflowMapper.update(workflowInstanceId, DAGNode.END.toString(), endState.value(), null, RUN_STATE_CANCELABLE, null);
                if (row < AFFECTED_ONE_ROW) {
                    log.warn("Update workflow end conflict: {} | {}", subInstance, endState);
                    return;
                }
                ends.forEach((k, v) -> graph.get(k.getTarget(), DAGNode.END).setRunState(endState.value()));
            }
        }

        Date now = new Date();

        // process workflows run state
        if (graph.allMatch(e -> e.getValue().isTerminal())) {
            RunState state = graph.anyMatch(e -> e.getValue().isFailure()) ? RunState.CANCELED : RunState.FINISHED;
            if (instanceMapper.terminate(workflowInstanceId, state.value(), RUN_STATE_CANCELABLE, now) == AFFECTED_ONE_ROW) {
                afterTerminateTask(instanceMapper.getByInstanceId(workflowInstanceId));
            } else {
                log.warn("Terminate workflow instance conflict: {} | {}", subInstance, state);
            }
            return;
        }

        Long jobId = workflowInstance.getJobId();
        SchedJob job = LazyLoader.of(SchedJob.class, jobMapper::getByJobId, jobId);
        DAGNode curNode = DAGNode.fromString(attach.getCurNode());
        // 查找当前节点的所有后继节点
        for (Map.Entry<DAGEdge, SchedWorkflow> node : graph.successors(curNode).entrySet()) {
            DAGNode target = node.getKey().getTarget();
            SchedWorkflow workflow = node.getValue();
            if (target.isEnd() || !RunState.WAITING.equals(workflow.getRunState())) {
                // 如果是结束节点 或 非WAITING状态，则跳过
                continue;
            }
            if (graph.predecessors(target).values().stream().anyMatch(e -> !RunState.FINISHED.equals(e.getRunState()))) {
                // 判断这个后续节点的所有前驱(依赖)节点有未执行完成的，则跳过
                continue;
            }

            long instanceId = generateId();
            row = workflowMapper.update(workflowInstanceId, target.toString(), RunState.RUNNING.value(), instanceId, RUN_STATE_WAITING, null);
            if (row < AFFECTED_ONE_ROW) {
                // 更新状态失败(冲突)，则跳过
                continue;
            }

            long triggerTime = workflowInstance.getTriggerTime() + workflow.getSequence();
            SchedInstance nextInstance = SchedInstance.create(instanceId, jobId, RunType.of(workflowInstance.getRunType()), triggerTime, 0, now);
            nextInstance.setRootInstanceId(subInstance.obtainRootInstanceId());
            nextInstance.setParentInstanceId(subInstance.getInstanceId());
            nextInstance.setWorkflowInstanceId(subInstance.getWorkflowInstanceId());
            nextInstance.setAttach(Jsons.toJson(new InstanceAttach(workflow.getCurNode())));
            try {
                List<SchedTask> tasks = splitTasks(SplitJobParam.from(job, target.getName()), nextInstance.getInstanceId(), new Date());
                // save to db
                instanceMapper.insert(nextInstance);
                Collects.batchProcess(tasks, taskMapper::batchInsert, PROCESS_BATCH_SIZE);
                TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(job, nextInstance, tasks));
            } catch (Exception e) {
                log.error("Split workflow job task error: " + subInstance, e);
            }
        }
    }

    private void retryJob(SchedInstance prev) {
        SchedJob schedJob = jobMapper.getByJobId(prev.getJobId());
        if (schedJob == null) {
            log.error("Sched job not found {}", prev.getJobId());
            processWorkflow(prev);
            return;
        }

        List<SchedTask> prevTasks = taskMapper.findLargeByInstanceId(prev.getInstanceId());
        RetryType retryType = RetryType.of(schedJob.getRetryType());
        if (retryType == RetryType.NONE || schedJob.getRetryCount() < 1) {
            // not retry
            processWorkflow(prev);
            return;
        }

        int retriedCount = Optional.ofNullable(prev.getRetriedCount()).orElse(0);
        if (retriedCount >= schedJob.getRetryCount()) {
            // already retried maximum times
            processWorkflow(prev);
            return;
        }

        // 如果是workflow，则需要更新sched_workflow.instance_id
        long retryInstanceId = generateId();
        if (prev.isWorkflowNode()) {
            String curNode = Jsons.fromJson(prev.getAttach(), InstanceAttach.class).getCurNode();
            int row = workflowMapper.update(prev.getWorkflowInstanceId(), curNode, null, retryInstanceId, RUN_STATE_RUNNING, prev.getInstanceId());
            if (row < AFFECTED_ONE_ROW) {
                return;
            }
        }

        // 1、build sched instance
        retriedCount++;
        Date now = new Date();
        long triggerTime = computeRetryTriggerTime(schedJob, retriedCount, now);
        SchedInstance retryInstance = SchedInstance.create(retryInstanceId, schedJob.getJobId(), RunType.RETRY, triggerTime, retriedCount, now);
        retryInstance.setRootInstanceId(prev.obtainRootInstanceId());
        retryInstance.setParentInstanceId(prev.getInstanceId());
        retryInstance.setWorkflowInstanceId(prev.getWorkflowInstanceId());
        retryInstance.setAttach(prev.getAttach());

        // 2、build sched tasks
        List<SchedTask> tasks;
        switch (retryType) {
            case ALL:
                try {
                    // re-split tasks
                    tasks = splitTasks(SplitJobParam.from(schedJob), retryInstance.getInstanceId(), now);
                } catch (Exception e) {
                    log.error("Split job error: " + schedJob + ", " + prev, e);
                    return;
                }
                break;
            case FAILED:
                tasks = prevTasks.stream()
                    .filter(e -> ExecuteState.of(e.getExecuteState()).isFailure())
                    // broadcast task cannot support partial retry
                    .filter(e -> !RouteStrategy.BROADCAST.equals(schedJob.getRouteStrategy()) || super.isAliveWorker(e.getWorker()))
                    .map(e -> SchedTask.create(e.getTaskParam(), generateId(), retryInstance.getInstanceId(), e.getTaskNo(), e.getTaskCount(), now, e.getWorker()))
                    .collect(Collectors.toList());
                break;
            default:
                log.error("Job unsupported retry type {}", schedJob);
                return;
        }

        // 3、save to db
        Assert.notEmpty(tasks, "Insert list of task cannot be empty.");
        instanceMapper.insert(retryInstance);
        Collects.batchProcess(tasks, taskMapper::batchInsert, PROCESS_BATCH_SIZE);

        TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(schedJob, retryInstance, tasks));
    }

    /**
     * Crates dependency job task.
     *
     * @param parentInstance the parent instance
     */
    private void dependJob(SchedInstance parentInstance) {
        List<SchedDepend> schedDepends = dependMapper.findByParentJobId(parentInstance.getJobId());
        if (CollectionUtils.isEmpty(schedDepends)) {
            return;
        }

        for (SchedDepend depend : schedDepends) {
            SchedJob childJob = jobMapper.getByJobId(depend.getChildJobId());
            if (childJob == null) {
                log.error("Child sched job not found: {} | {}", depend.getParentJobId(), depend.getChildJobId());
                continue;
            }
            if (JobState.DISABLE.equals(childJob.getJobState())) {
                continue;
            }

            try {
                TriggerInstanceCreator creator = TriggerInstanceCreator.of(childJob.getJobType(), this);
                TriggerInstance tInstance = creator.create(childJob, RunType.DEPEND, parentInstance.getTriggerTime());
                tInstance.getInstance().setRootInstanceId(parentInstance.obtainRootInstanceId());
                tInstance.getInstance().setParentInstanceId(parentInstance.getInstanceId());

                createInstance(tInstance);
                TransactionUtils.doAfterTransactionCommit(() -> creator.dispatch(childJob, tInstance));
            } catch (Exception e) {
                log.error("Depend job split failed: " + childJob, e);
            }
        }
    }

    private List<ExecuteTaskParam> loadExecutingTasks(SchedInstance instance, Operations ops) {
        List<ExecuteTaskParam> executingTasks = new ArrayList<>();
        ExecuteTaskParamBuilder builder = ExecuteTaskParam.builder(instance, jobMapper::getByJobId);
        // immediate trigger
        long triggerTime = 0L;
        taskMapper.findMediumByInstanceId(instance.getInstanceId())
            .stream()
            .filter(e -> ExecuteState.EXECUTING.equals(e.getExecuteState()))
            .forEach(task -> {
                Worker worker = Worker.deserialize(task.getWorker());
                if (super.isAliveWorker(worker)) {
                    executingTasks.add(builder.build(ops, task.getTaskId(), triggerTime, worker));
                } else {
                    // update dead task
                    Date executeEndTime = ops.toState().isTerminal() ? new Date() : null;
                    int row = taskMapper.terminate(task.getTaskId(), ops.toState().value(), ExecuteState.EXECUTING.value(), executeEndTime, null);
                    if (row != AFFECTED_ONE_ROW) {
                        log.warn("Cancel the dead task failed: {}", task);
                        executingTasks.add(builder.build(ops, task.getTaskId(), triggerTime, worker));
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
            dependMapper.batchInsert(parentJobIds.stream().map(e -> new SchedDepend(e, job.getJobId())).collect(Collectors.toList()));
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
