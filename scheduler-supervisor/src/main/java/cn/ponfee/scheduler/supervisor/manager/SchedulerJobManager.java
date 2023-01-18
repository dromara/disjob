/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.manager;

import cn.ponfee.scheduler.common.base.Constants;
import cn.ponfee.scheduler.common.base.IdGenerator;
import cn.ponfee.scheduler.common.base.LazyLoader;
import cn.ponfee.scheduler.common.base.tuple.Tuple3;
import cn.ponfee.scheduler.common.spring.MarkRpcController;
import cn.ponfee.scheduler.common.spring.TransactionUtils;
import cn.ponfee.scheduler.common.util.Collects;
import cn.ponfee.scheduler.core.base.SupervisorService;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.enums.*;
import cn.ponfee.scheduler.core.exception.JobException;
import cn.ponfee.scheduler.core.model.SchedDepend;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.core.model.SchedTrack;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.dispatch.TaskDispatcher;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import cn.ponfee.scheduler.supervisor.base.WorkerServiceClient;
import cn.ponfee.scheduler.supervisor.dao.mapper.SchedDependMapper;
import cn.ponfee.scheduler.supervisor.dao.mapper.SchedJobMapper;
import cn.ponfee.scheduler.supervisor.dao.mapper.SchedTaskMapper;
import cn.ponfee.scheduler.supervisor.dao.mapper.SchedTrackMapper;
import com.google.common.base.Joiner;
import com.google.common.math.IntMath;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

import static cn.ponfee.scheduler.common.base.Constants.TX_MANAGER_SUFFIX;
import static cn.ponfee.scheduler.supervisor.dao.SchedulerDataSourceConfig.DB_NAME;

/**
 * Manage Schedule job.
 *
 * <p>Spring事务提交后执行一些后置操作
 *
 * <pre>方案1: {@code
 *  TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
 *      @Override
 *      public void afterCommit() {
 *          dispatch(job, track, tasks);
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
 *    // some database operation code ....
 *    eventPublisher.publishEvent(new DispatchTaskEvent(() -> dispatch(job, track, tasks)));
 *    // others operation code ....
 *  }
 * }</pre>
 *
 * @author Ponfee
 */
@Component
public class SchedulerJobManager extends AbstractSupervisorManager implements SupervisorService, MarkRpcController {

    private static final String TX_MANAGER_NAME = DB_NAME + TX_MANAGER_SUFFIX;
    private static final int AFFECTED_ONE_ROW = 1;
    private static final String DEFAULT_USER = "0";
    private static final List<Integer> CANCELABLE_RUN_STATE_LIST = Collects.convert(RunState.CANCELABLE_LIST, RunState::value);
    private static final List<Integer> EXECUTABLE_EXECUTE_STATE_LIST = Collects.convert(ExecuteState.EXECUTABLE_LIST, ExecuteState::value);

    private final SchedJobMapper jobMapper;
    private final SchedTrackMapper trackMapper;
    private final SchedTaskMapper taskMapper;
    private final SchedDependMapper dependMapper;

    public SchedulerJobManager(IdGenerator idGenerator,
                               SupervisorRegistry discoveryWorker,
                               TaskDispatcher taskDispatcher,
                               WorkerServiceClient workerServiceClient,
                               SchedJobMapper jobMapper,
                               SchedTrackMapper trackMapper,
                               SchedTaskMapper taskMapper,
                               SchedDependMapper dependMapper) {
        super(idGenerator, discoveryWorker, taskDispatcher, workerServiceClient);
        this.jobMapper = jobMapper;
        this.trackMapper = trackMapper;
        this.taskMapper = taskMapper;
        this.dependMapper = dependMapper;
    }

    // ------------------------------------------------------------------database query

    @Override
    public SchedJob getJob(long jobId) {
        return jobMapper.getByJobId(jobId);
    }

    public SchedTrack getTrack(long trackId) {
        return trackMapper.getByTrackId(trackId);
    }

    @Override
    public SchedTask getTask(long taskId) {
        return taskMapper.getByTaskId(taskId);
    }

    public List<SchedTask> findMediumTaskByTrackId(long trackId) {
        return taskMapper.findMediumByTrackId(trackId);
    }

    public List<SchedTask> findLargeTaskByTrackId(long trackId) {
        return taskMapper.findLargeByTrackId(trackId);
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

    public List<SchedTrack> findExpireWaiting(Date expireTime, int size) {
        return trackMapper.findExpireState(RunState.WAITING.value(), expireTime.getTime(), expireTime, size);
    }

    public List<SchedTrack> findExpireRunning(Date expireTime, int size) {
        return trackMapper.findExpireState(RunState.RUNNING.value(), expireTime.getTime(), expireTime, size);
    }

    public SchedTrack getByTriggerTime(long jobId, long triggerTime, int runType) {
        return trackMapper.getByTriggerTime(jobId, triggerTime, runType);
    }

    public List<SchedTrack> findUnterminatedRetry(long trackId) {
        return trackMapper.findUnterminatedRetry(trackId);
    }

    // ------------------------------------------------------------------database single operation without @Transactional annotation

    @Override
    public boolean checkpoint(long taskId, String executeSnapshot) {
        return taskMapper.checkpoint(taskId, executeSnapshot) == AFFECTED_ONE_ROW;
    }

    public boolean renewUpdateTime(SchedTrack track, Date updateTime) {
        return trackMapper.renewUpdateTime(track.getTrackId(), updateTime, track.getVersion()) == AFFECTED_ONE_ROW;
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

    @Override
    public boolean updateTaskWorker(List<Long> taskIds, String worker) {
        if (StringUtils.isNotBlank(worker)) {
            try {
                Worker.deserialize(worker);
            } catch (Exception e) {
                log.error("Invalid worker serialized text: {}", worker);
                return false;
            }
        }
        return taskMapper.updateWorker(taskIds, worker) >= AFFECTED_ONE_ROW;
    }

    @Override
    public boolean updateTaskErrorMsg(long taskId, String errorMsg) {
        try {
            return taskMapper.updateErrorMsg(taskId, errorMsg) == AFFECTED_ONE_ROW;
        } catch (Exception e) {
            log.error("Update sched task error msg failed: " + taskId, e);
            return false;
        }
    }

    // ------------------------------------------------------------------operation within transaction

    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    public void addJob(SchedJob job) {
        Assert.notNull(job.getTriggerType(), "Trigger type cannot be null.");
        Assert.notNull(job.getTriggerConf(), "Trigger conf cannot be null.");
        Assert.isNull(job.getLastTriggerTime(), "Last trigger time must be null.");
        Assert.isNull(job.getNextTriggerTime(), "Next trigger time must be null.");
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
        Assert.notNull(job.getJobId(), "Job id cannot be null");
        Assert.notNull(job.getVersion(), "Version cannot be null");
        Assert.isNull(job.getLastTriggerTime(), "Last trigger time must be null");
        Assert.isNull(job.getNextTriggerTime(), "Next trigger time must be null.");
        if (StringUtils.isEmpty(job.getJobHandler())) {
            Assert.isTrue(StringUtils.isEmpty(job.getJobParam()), "Job param must be null if not set job handler.");
        } else {
            super.verifyJobHandler(job);
        }

        job.checkAndDefaultSetting();

        SchedJob dbSchedJob = jobMapper.getByJobId(job.getJobId());
        Assert.notNull(dbSchedJob, "Sched job id not found " + job.getJobId());
        job.setNextTriggerTime(dbSchedJob.getNextTriggerTime());

        Date now = new Date();
        if (job.getTriggerType() == null) {
            Assert.isNull(job.getTriggerConf(), "Trigger conf must be null if not set trigger type.");
        } else {
            Assert.notNull(job.getTriggerConf(), "Trigger conf cannot be null if has set trigger type.");
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

    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    public void deleteTrack(long trackId) {
        SchedTrack track = trackMapper.getByTrackId(trackId);
        Assert.notNull(track, "Sched track not found: " + trackId);

        RunState runState = RunState.of(track.getRunState());
        Assert.isTrue(runState.isTerminal(), "Cannot delete unterminated sched track: " + trackId + ", run state=" + runState);

        int row = trackMapper.deleteByTrackId(trackId);
        Assert.isTrue(row == AFFECTED_ONE_ROW, "Delete sched track conflict: " + trackId);

        taskMapper.deleteByTrackId(trackId);
    }

    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    public void forceUpdateState(long trackId, int trackTargetState, int taskTargetState) {
        ExecuteState taskTargetState0 = ExecuteState.of(taskTargetState);
        Assert.isTrue(taskTargetState0.runState() == RunState.of(trackTargetState), "Inconsistent state: " + trackTargetState + ", " + taskTargetState);
        int row = trackMapper.forceUpdateState(trackId, trackTargetState);
        Assert.isTrue(row == AFFECTED_ONE_ROW, "Sched track state update failed " + trackId);

        row = taskMapper.forceUpdateState(trackId, taskTargetState);
        Assert.isTrue(row >= AFFECTED_ONE_ROW, "Sched task state update failed, track_id=" + trackId);

        if (taskTargetState0 == ExecuteState.WAITING) {
            Tuple3<SchedJob, SchedTrack, List<SchedTask>> params = buildDispatchParams(trackId, row);
            TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(params.a, params.b, params.c));
        }
    }

    /**
     * Manual trigger the sched job
     *
     * @param jobId the job id
     * @throws JobException if occur error
     */
    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    public void trigger(long jobId) throws JobException {
        SchedJob job = jobMapper.getByJobId(jobId);
        Assert.notNull(job, "Sched job not found: " + jobId);

        // 1、build sched track and sched task list
        Date now = new Date();
        SchedTrack track = SchedTrack.create(
            generateId(), job.getJobId(), RunType.MANUAL, now.getTime(), 0, now
        );
        List<SchedTask> tasks = splitTasks(job, track.getTrackId(), now);

        // 2、save sched trace and sched task to database
        int row = trackMapper.insert(track);
        Assert.state(row == AFFECTED_ONE_ROW, "Insert sched track fail: " + track);

        row = taskMapper.insertBatch(tasks);
        Assert.state(row == tasks.size(), "Insert sched task fail: " + tasks);

        // 3、dispatch job task
        TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(job, track, tasks));
    }

    /**
     * Update sched job, and save one sched track and many tasks.
     *
     * @param job   the job
     * @param track the track
     * @param tasks the tasks
     * @return if {@code true} operated success
     */
    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    public boolean updateAndSave(SchedJob job, SchedTrack track, List<SchedTask> tasks) {
        int row = jobMapper.updateNextTriggerTime(job);
        if (row == 0) {
            // conflict operation, need not process
            return false;
        }

        row = trackMapper.insert(track);
        Assert.state(row == AFFECTED_ONE_ROW, "Insert sched track fail: " + track);

        Assert.notEmpty(tasks, "Insert list of task cannot be empty.");
        row = taskMapper.insertBatch(tasks);
        Assert.state(row == tasks.size(), "Insert sched task fail: " + tasks);
        return true;
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
        Integer state = trackMapper.getStateByTrackId(param.getTrackId());
        Assert.state(state != null, "Sched track not found: " + param);
        RunState runState = RunState.of(state);
        // sched_track.run_state must in (WAITING, RUNNING)
        Assert.state(RunState.PAUSABLE_LIST.contains(runState), "Start track failed: " + param + ", " + runState);

        Date now = new Date();
        // start sched track(also possibly started by other task)
        int trackRow = trackMapper.start(param.getTrackId(), now);

        // start sched task
        int taskRow = taskMapper.start(param.getTaskId(), param.getWorker().toString(), now);

        if (trackRow == 0 && taskRow == 0) {
            // conflict: the task executed by other executor
            return false;
        } else {
            Assert.state(taskRow == AFFECTED_ONE_ROW, "Start task failed: " + param);
            return true;
        }
    }

    /**
     * Terminate the running task.
     *
     * @param param    the taskParam
     * @param toState  the toState
     * @param errorMsg the errorMsg
     * @return {@code true} if termination was successful
     */
    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    @Override
    public boolean terminateExecutingTask(ExecuteParam param, ExecuteState toState, String errorMsg) {
        Integer state = trackMapper.lockAndGetState(param.getTrackId());
        Assert.notNull(state, "Terminate failed, track_id not found: " + param.getTrackId());
        if (RunState.of(state).isTerminal()) {
            // already terminated
            return false;
        }
        int row = taskMapper.terminate(
            param.getTaskId(),
            toState.value(),
            ExecuteState.EXECUTING.value(),
            new Date(),
            errorMsg
        );
        boolean result = (row == AFFECTED_ONE_ROW);
        if (!result) {
            log.warn("Conflict terminate task {}, {}", param.getTaskId(), toState);
        }

        // terminate track
        terminate(param.getTrackId(), false);
        return result;
    }

    /**
     * Terminate the running track and task.
     *
     * @param trackId the trackId
     * @return if {@code true} terminate successfully
     */
    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    public boolean terminate(long trackId) {
        Integer state = trackMapper.lockAndGetState(trackId);
        Assert.notNull(state, "Terminate failed, track_id not found: " + trackId);
        if (RunState.of(state).isTerminal()) {
            // already terminated
            return false;
        }
        return terminate(trackId, true);
    }

    // ------------------------------------------------------------------manual pause

    /**
     * Pause task by sched track id
     *
     * @param trackId the track id
     * @return {@code true} if paused successfully
     */
    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    @Override
    public boolean pauseTrack(long trackId) {
        Integer state = trackMapper.lockAndGetState(trackId);
        Assert.notNull(state, "Pause failed, track_id not found: " + trackId);

        RunState runState = RunState.of(state);
        if (!RunState.PAUSABLE_LIST.contains(runState)) {
            return false;
        }

        // update waiting task
        taskMapper.updateStateByTrackId(
            trackId,
            ExecuteState.PAUSED.value(), 
            Collections.singletonList(ExecuteState.WAITING.value()), 
            null
        );

        // load the alive executing tasks
        List<ExecuteParam> executingTasks = loadExecutingTasks(trackId, Operations.PAUSE);

        if (executingTasks.isEmpty()) {
            // has non executing execute_state, update sched track state
            List<ExecuteState> stateList = taskMapper.findMediumByTrackId(trackId)
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
                row = trackMapper.terminate(
                    trackId,
                    toRunState.value(), 
                    Collections.singletonList(runState.value()), 
                    new Date()
                );
            } else {
                row = trackMapper.updateState(trackId, toRunState.value(), runState.value(), null);
            }
            if (row != AFFECTED_ONE_ROW) {
                log.warn("Pause track from {} to {} conflict", runState, toRunState);
            }
        } else {
            // dispatch and pause executing tasks
            TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(executingTasks));
        }

        return true;
    }

    /**
     * Pause executing task
     *
     * @param param    the execution param
     * @param errorMsg the execution error message
     * @return {@code true} if paused successfully
     */
    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    @Override
    public boolean pauseExecutingTask(ExecuteParam param, String errorMsg) {
        Integer state = trackMapper.lockAndGetState(param.getTrackId());
        if (!RunState.RUNNING.equals(state)) {
            log.warn("Pause executing task failed: {} | {}", param, state);
            return false;
        }

        int row = taskMapper.updateState(
            param.getTaskId(), 
            ExecuteState.PAUSED.value(), 
            ExecuteState.EXECUTING.value(), 
            errorMsg,
            null
        );
        if (row != AFFECTED_ONE_ROW) {
            log.warn("Paused task unsuccessful.");
            return false;
        }

        boolean allPaused = taskMapper.findMediumByTrackId(param.getTrackId())
                                      .stream()
                                      .map(e -> ExecuteState.of(e.getExecuteState()))
                                      .noneMatch(ExecuteState.PAUSABLE_LIST::contains);
        if (allPaused) {
            row = trackMapper.updateState(
                param.getTrackId(), RunState.PAUSED.value(), RunState.RUNNING.value(), null
            );
            if (row != AFFECTED_ONE_ROW) {
                log.error("Update sched track to paused state conflict: {} | {}", param.getTrackId(), param.getTaskId());
            }
        }

        return true;
    }

    // ------------------------------------------------------------------manual cancel

    /**
     * Cancel task by sched track id
     *
     * @param trackId   the track id
     * @param operation the operation
     * @return {@code true} if canceled successfully
     */
    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    @Override
    public boolean cancelTrack(long trackId, Operations operation) {
        Assert.isTrue(operation.targetState().isFailure(), "Expect cancel ops, but actual: " + operation);
        Integer state = trackMapper.lockAndGetState(trackId);
        Assert.notNull(state, "Cancel failed, track_id not found: " + trackId);

        RunState runState = RunState.of(state);
        if (runState.isTerminal()) {
            return false;
        }

        // update waiting & paused state task
        taskMapper.updateStateByTrackId(
            trackId,
            operation.targetState().value(),
            EXECUTABLE_EXECUTE_STATE_LIST, 
            new Date()
        );

        // load the alive executing tasks
        List<ExecuteParam> executingTasks = loadExecutingTasks(trackId, operation);

        if (executingTasks.isEmpty()) {
            // has non executing execute_state
            boolean failure = taskMapper.findMediumByTrackId(trackId)
                                        .stream()
                                        .anyMatch(e -> ExecuteState.of(e.getExecuteState()).isFailure());
            RunState toRunState = failure ? RunState.CANCELED : RunState.FINISHED;
            int row = trackMapper.terminate(
                trackId,
                toRunState.value(), 
                Collections.singletonList(runState.value()), 
                new Date()
            );
            if (row != AFFECTED_ONE_ROW) {
                log.warn("Pause track from {} to {} conflict", runState, toRunState);
            }
        } else {
            // dispatch and cancel executing tasks
            TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(executingTasks));
        }

        return true;
    }

    /**
     * Cancel executing task
     *
     * @param param    the execution param
     * @param toState  the target state
     * @param errorMsg the executed error message
     * @return {@code true} if canceled successfully
     */
    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    @Override
    public boolean cancelExecutingTask(ExecuteParam param, ExecuteState toState, String errorMsg) {
        Assert.isTrue(toState.isFailure(), "Target state expect failure state, but actual: " + toState);
        Integer state = trackMapper.lockAndGetState(param.getTrackId());
        if (!RunState.RUNNING.equals(state)) {
            return false;
        }

        int row = taskMapper.terminate(
            param.getTaskId(),
            toState.value(),
            ExecuteState.EXECUTING.value(),
            new Date(),
            errorMsg
        );
        if (row != AFFECTED_ONE_ROW) {
            log.warn("Canceled task unsuccessful.");
            return false;
        }

        boolean allTerminated = taskMapper.findMediumByTrackId(param.getTrackId())
                                          .stream()
                                          .map(e -> ExecuteState.of(e.getExecuteState()))
                                          .allMatch(ExecuteState::isTerminal);
        if (allTerminated) {
            row = trackMapper.terminate(
                param.getTrackId(),
                RunState.CANCELED.value(),
                Collections.singletonList(RunState.RUNNING.value()),
                new Date()
            );
            if (row != AFFECTED_ONE_ROW) {
                log.error("Update sched track to canceled state conflict: {} | {}", param.getTrackId(), param.getTaskId());
            }
        }
        return true;
    }

    /**
     * Resume sched track from paused state to waiting state
     *
     * @param trackId the trackId
     * @return {@code true} if resumed successfully
     */
    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    public boolean resume(long trackId) {
        Integer state = trackMapper.lockAndGetState(trackId);
        Assert.notNull(state, "Cancel failed, track_id not found: " + trackId);
        if (!RunState.PAUSED.equals(state)) {
            return false;
        }

        int row = trackMapper.updateState(trackId, RunState.WAITING.value(), RunState.PAUSED.value(), null);
        Assert.state(row == AFFECTED_ONE_ROW, "Resume sched track failed.");

        row = taskMapper.updateStateByTrackId(
            trackId,
            ExecuteState.WAITING.value(), 
            Collections.singletonList(ExecuteState.PAUSED.value()), 
            null
        );
        Assert.state(row >= AFFECTED_ONE_ROW, "Resume sched task failed.");

        // dispatch task
        Tuple3<SchedJob, SchedTrack, List<SchedTask>> params = buildDispatchParams(trackId, row);
        TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(params.a, params.b, params.c));

        return true;
    }

    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    public boolean updateState(ExecuteState toState, List<SchedTask> tasks, SchedTrack track) {
        if (trackMapper.lockAndGetId(track.getTrackId()) == null) {
            return false;
        }
        int row = trackMapper.updateState(track.getTrackId(), toState.runState().value(), track.getRunState(), track.getVersion());
        if (row != AFFECTED_ONE_ROW) {
            log.warn("Conflict update track run state: {} | {}", track, toState.runState());
            return false;
        }

        row = 0;
        for (SchedTask task : tasks) {
            row += taskMapper.updateState(
                task.getTaskId(),
                toState.value(),
                task.getExecuteState(),
                null,
                task.getVersion()
            );
        }
        Assert.state(row >= AFFECTED_ONE_ROW, "Conflict update state: " + toState + ", " + tasks + ", " + track);

        // updated successfully
        return true;
    }

    // ------------------------------------------------------------------private methods

    /**
     * Terminate the running track and task.
     *
     * @param trackId       the trackId
     * @param force         is whether force terminate
     * @return {@code true} if terminate successfully
     */
    private boolean terminate(long trackId, boolean force) {
        List<SchedTask> tasks = taskMapper.findMediumByTrackId(trackId);
        if (CollectionUtils.isEmpty(tasks)) {
            // cannot happen
            log.error("Not found sched track task data {}", trackId);
            return false;
        }

        List<ExecuteState> taskStateList = tasks.stream()
                                                .map(SchedTask::getExecuteState)
                                                .map(ExecuteState::of)
                                                .collect(Collectors.toList());

        Date runEndTime;
        RunState runState;
        if (taskStateList.stream().allMatch(ExecuteState::isTerminal)) {
            runEndTime = tasks.stream()
                              .map(SchedTask::getExecuteEndTime)
                              .max(Comparator.naturalOrder())
                              .orElseThrow(IllegalStateException::new);
            runState = taskStateList.stream().allMatch(ExecuteState.FINISHED::equals)
                      ? RunState.FINISHED
                      : RunState.CANCELED;
        } else {
            if (force) {
                runEndTime = new Date();
                runState = RunState.CANCELED;
            } else {
                return false;
            }
        }

        int row = trackMapper.terminate(trackId, runState.value(), CANCELABLE_RUN_STATE_LIST, runEndTime);
        if (row != AFFECTED_ONE_ROW) {
            return false;
        }

        if (force) {
            tasks.stream()
                 .filter(e -> !ExecuteState.of(e.getExecuteState()).isTerminal())
                 .forEach(e -> {
                     int affectedRow = taskMapper.terminate(e.getTaskId(), ExecuteState.EXECUTE_TIMEOUT.value(), e.getExecuteState(), new Date(), null);
                     Assert.state(affectedRow == AFFECTED_ONE_ROW, "Terminate task state conflict " + e);
                 });
        }

        if (runState == RunState.CANCELED) {
            retryJob(trackId);
        } else if (runState == RunState.FINISHED) {
            dependJob(trackId);
        } else {
            // cannot happen
            log.error("Unknown retry run state " + runState);
        }

        return true;
    }

    private void retryJob(long trackId) {
        SchedTrack prevTrack = trackMapper.getByTrackId(trackId);
        SchedJob schedJob = jobMapper.getByJobId(prevTrack.getJobId());
        if (schedJob == null) {
            log.error("Sched job not found {}", prevTrack.getJobId());
            return;
        }

        List<SchedTask> prevTasks = taskMapper.findLargeByTrackId(trackId);
        RetryType retryType = RetryType.of(schedJob.getRetryType());
        if (retryType == RetryType.NONE || schedJob.getRetryCount() < 1) {
            // not retry
            return;
        }

        int retriedCount = Optional.ofNullable(prevTrack.getRetriedCount()).orElse(0);
        if (retriedCount >= schedJob.getRetryCount()) {
            // already retried maximum times
            return;
        }

        Date now = new Date();

        // 1、build sched track
        retriedCount++;
        long triggerTime = computeRetryTriggerTime(schedJob, retriedCount, now);
        SchedTrack retryTrack = SchedTrack.create(
            generateId(), schedJob.getJobId(), RunType.RETRY, triggerTime, retriedCount, now
        );
        retryTrack.setParentTrackId(RunType.RETRY.equals(prevTrack.getRunType()) ? prevTrack.getParentTrackId() : prevTrack.getTrackId());

        // 2、build sched tasks
        List<SchedTask> tasks;
        switch (retryType) {
            case ALL:
                try {
                    // re-split tasks
                    tasks = splitTasks(schedJob, retryTrack.getTrackId(), now);
                } catch (Exception e) {
                    log.error("Split job error: " + schedJob + ", " + prevTrack, e);
                    return;
                }
                break;
            case FAILED:
                tasks = prevTasks.stream()
                                 .filter(e -> ExecuteState.of(e.getExecuteState()).isFailure())
                                 .map(e -> SchedTask.create(e.getTaskParam(), generateId(), retryTrack.getTrackId(), now))
                                 .collect(Collectors.toList());
                break;
            default:
                log.error("Job unsupported retry type {}", schedJob);
                return;
        }

        // 3、save to db
        Assert.notEmpty(tasks, "Insert list of task cannot be empty.");
        trackMapper.insert(retryTrack);
        taskMapper.insertBatch(tasks);

        TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(schedJob, retryTrack, tasks));
    }

    /**
     * Crates dependency jbo task.
     *
     * @param trackId the parent trace id
     */
    private void dependJob(long trackId) {
        SchedTrack parentTrack = trackMapper.getByTrackId(trackId);
        List<SchedDepend> schedDepends = dependMapper.findByParentJobId(parentTrack.getJobId());
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
                SchedTrack track = SchedTrack.create(
                    generateId(), childJob.getJobId(), RunType.DEPEND, parentTrack.getTriggerTime(), 0, now
                );
                track.setParentTrackId(parentTrack.getTrackId());
                List<SchedTask> tasks = splitTasks(childJob, track.getTrackId(), now);

                // save to db
                trackMapper.insert(track);
                taskMapper.insertBatch(tasks);

                TransactionUtils.doAfterTransactionCommit(() -> super.dispatch(childJob, track, tasks));
            } catch (Exception e) {
                log.error("Depend job split failed: " + childJob, e);
            }
        }
    }

    private List<ExecuteParam> loadExecutingTasks(long trackId, Operations ops) {
        SchedTrack schedTrackProxy = LazyLoader.of(SchedTrack.class, trackMapper::getByTrackId, trackId);
        List<ExecuteParam> executingTasks = new ArrayList<>();
        taskMapper.findMediumByTrackId(trackId)
            .stream()
            .filter(e -> ExecuteState.EXECUTING.equals(e.getExecuteState()))
            .forEach(task -> {
                Worker worker = Worker.deserialize(task.getWorker());
                if (super.isAliveWorker(worker)) {
                    ExecuteParam param = new ExecuteParam(ops, task.getTaskId(), trackId, schedTrackProxy.getJobId(), 0L);
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

    private Tuple3<SchedJob, SchedTrack, List<SchedTask>> buildDispatchParams(long trackId, int expectTaskSize) {
        SchedTrack track = trackMapper.getByTrackId(trackId);
        SchedJob job = jobMapper.getByJobId(track.getJobId());
        List<SchedTask> waitingTasks = taskMapper.findLargeByTrackId(trackId)
            .stream()
            .filter(e -> ExecuteState.WAITING.equals(e.getExecuteState()))
            .collect(Collectors.toList());
        Assert.isTrue(waitingTasks.size() == expectTaskSize, "Dispatching tasks size inconsistent, expect=" + expectTaskSize + ", actual=" + waitingTasks.size());
        return Tuple3.of(job, track, waitingTasks);
    }

    private void parseTriggerConfig(SchedJob job, Date date) {
        TriggerType triggerType = TriggerType.of(job.getTriggerType());
        Assert.isTrue(
            triggerType.isValid(job.getTriggerConf()),
            "Invalid trigger config: " + job.getTriggerType() + ", " + job.getTriggerConf()
        );

        if (triggerType == TriggerType.DEPEND) {
            List<Long> parentJobIds = Arrays.stream(job.getTriggerConf().split(Constants.COMMA))
                                            .filter(StringUtils::isNotBlank)
                                            .map(e -> Long.parseLong(e.trim()))
                                            .distinct()
                                            .collect(Collectors.toList());
            Assert.isTrue(
                !parentJobIds.isEmpty() && jobMapper.countJobIds(parentJobIds) == parentJobIds.size(),
                "Has parent job id not found " + job.getTriggerConf()
            );
            dependMapper.insertBatch(
                parentJobIds.stream().map(e -> new SchedDepend(e, job.getJobId())).collect(Collectors.toList())
            );
            job.setTriggerConf(Joiner.on(Constants.COMMA).join(parentJobIds));
            job.setNextTriggerTime(null);
        } else {
            Date nextTriggerTime = triggerType.computeNextFireTime(job.getTriggerConf(), date);
            Assert.notNull(nextTriggerTime, "Has not next trigger time " + job.getTriggerConf());
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
        Assert.isTrue(!RetryType.NONE.equals(job.getRetryType()), "Sched job '" + job.getJobId() + "' retry type is NONE.");
        Assert.isTrue(job.getRetryCount() > 0, "Sched job '" + job.getJobId() + "' retry count must greater than 0, but actual " + job.getRetryCount());
        Assert.isTrue(failCount <= job.getRetryCount(), "Sched job '" + job.getJobId() + "' retried " + failCount + " exceed " + job.getRetryCount() + " limit.");
        // exponential backoff
        return current.getTime() + (long) job.getRetryInterval() * IntMath.pow(failCount, 2);
    }

}
