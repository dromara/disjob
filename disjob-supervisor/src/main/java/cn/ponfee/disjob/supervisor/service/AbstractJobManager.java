/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.service;

import cn.ponfee.disjob.common.base.IdGenerator;
import cn.ponfee.disjob.common.base.Symbol.Str;
import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.core.base.JobCodeMsg;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.*;
import cn.ponfee.disjob.core.exception.JobCheckedException;
import cn.ponfee.disjob.core.handle.SplitTask;
import cn.ponfee.disjob.core.model.SchedDepend;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.core.param.ExecuteTaskParam;
import cn.ponfee.disjob.core.param.JobHandlerParam;
import cn.ponfee.disjob.dispatch.TaskDispatcher;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.supervisor.base.WorkerCoreRpcClient;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedDependMapper;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedJobMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static cn.ponfee.disjob.supervisor.base.AbstractDataSourceConfig.TX_MANAGER_NAME_SUFFIX;
import static cn.ponfee.disjob.supervisor.dao.SupervisorDataSourceConfig.DB_NAME;

/**
 * The base job manager
 *
 * @author Ponfee
 */
@RequiredArgsConstructor
public abstract class AbstractJobManager {

    private static final int MAX_SPLIT_TASK_SIZE = 1000;
    private static final List<TriggerType> FIXED_TYPES = ImmutableList.of(TriggerType.FIXED_RATE, TriggerType.FIXED_DELAY);
    private static final int AFFECTED_ONE_ROW = 1;

    protected static final String TX_MANAGER_NAME = DB_NAME + TX_MANAGER_NAME_SUFFIX;

    protected final SchedJobMapper jobMapper;
    protected final SchedDependMapper dependMapper;

    private final IdGenerator idGenerator;
    private final SupervisorRegistry workerDiscover;
    private final TaskDispatcher taskDispatcher;
    private final WorkerCoreRpcClient workerCoreRpcClient;

    // ------------------------------------------------------------------database single operation without spring transactional

    public boolean disableJob(SchedJob job) {
        return isOneAffectedRow(jobMapper.disable(job));
    }

    public boolean changeJobState(long jobId, JobState to) {
        boolean flag = isOneAffectedRow(jobMapper.updateState(jobId, to.value(), 1 ^ to.value()));
        SchedJob job;
        if (flag && to == JobState.ENABLE && TriggerType.FIXED_DELAY.equals((job = jobMapper.get(jobId)).getTriggerType())) {
            Date date = null;
            if (job.getLastTriggerTime() != null) {
                date = TriggerType.FIXED_DELAY.computeNextTriggerTime(job.getTriggerValue(), new Date(job.getLastTriggerTime()));
            }
            Date nextTriggerTime = Dates.max(new Date(), job.getStartTime(), date);
            jobMapper.updateFixedDelayNextTriggerTime(jobId, nextTriggerTime.getTime());
        }
        return flag;
    }

    public boolean updateJobNextTriggerTime(SchedJob job) {
        return isOneAffectedRow(jobMapper.updateNextTriggerTime(job));
    }

    public boolean updateJobNextScanTime(SchedJob schedJob) {
        return isOneAffectedRow(jobMapper.updateNextScanTime(schedJob));
    }

    // ------------------------------------------------------------------database operation within spring transactional

    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    public void addJob(SchedJob job) throws JobCheckedException {
        job.setUpdatedBy(job.getCreatedBy());
        job.verifyBeforeAdd();
        job.checkAndDefaultSetting();
        workerCoreRpcClient.verify(JobHandlerParam.from(job));
        job.setJobId(generateId());
        parseTriggerConfig(job, false);

        jobMapper.insert(job);
    }

    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    public void updateJob(SchedJob job) throws JobCheckedException {
        job.verifyBeforeUpdate();
        job.checkAndDefaultSetting();
        if (StringUtils.isEmpty(job.getJobHandler())) {
            Assert.hasText(job.getJobParam(), "Job param must be null if not set job handler.");
        } else {
            workerCoreRpcClient.verify(JobHandlerParam.from(job));
        }

        SchedJob dbJob = jobMapper.get(job.getJobId());
        Assert.notNull(dbJob, () -> "Sched job id not found " + job.getJobId());
        job.setNextTriggerTime(dbJob.getNextTriggerTime());

        if (job.getTriggerType() == null) {
            Assert.isNull(job.getTriggerValue(), "Trigger value must be null if not set trigger type.");
        } else if (!dbJob.getTriggerType().equals(job.getTriggerType()) || !dbJob.getTriggerValue().equals(job.getTriggerValue())) {
            Assert.notNull(job.getTriggerValue(), "Trigger value cannot be null if has set trigger type.");
            // update last trigger time or depends parent job id
            dependMapper.deleteByChildJobId(job.getJobId());
            parseTriggerConfig(job, true);
        }

        job.setUpdatedAt(new Date());
        assertOneAffectedRow(jobMapper.update(job), "Update sched job fail or conflict.");
    }

    @Transactional(transactionManager = TX_MANAGER_NAME, rollbackFor = Exception.class)
    public void deleteJob(long jobId) {
        SchedJob job = jobMapper.get(jobId);
        Assert.notNull(job, "Job id not found: " + jobId);
        if (JobState.ENABLE.equals(job.getJobState())) {
            throw new IllegalStateException("Please disable job before delete this job.");
        }
        assertOneAffectedRow(jobMapper.softDelete(jobId), "Delete sched job fail or conflict.");
        dependMapper.deleteByParentJobId(jobId);
        dependMapper.deleteByChildJobId(jobId);
    }

    // ------------------------------------------------------------------others operation

    protected boolean isOneAffectedRow(int totalAffectedRow) {
        return totalAffectedRow == AFFECTED_ONE_ROW;
    }

    protected boolean isManyAffectedRow(int totalAffectedRow) {
        return totalAffectedRow >= AFFECTED_ONE_ROW;
    }

    protected void assertOneAffectedRow(int totalAffectedRow, Supplier<String> errorMsgSupplier) {
        if (totalAffectedRow != AFFECTED_ONE_ROW) {
            throw new IllegalStateException(errorMsgSupplier.get());
        }
    }

    protected void assertOneAffectedRow(int totalAffectedRow, String errorMsg) {
        if (totalAffectedRow != AFFECTED_ONE_ROW) {
            throw new IllegalStateException(errorMsg);
        }
    }

    protected void assertManyAffectedRow(int totalAffectedRow, Supplier<String> errorMsgSupplier) {
        if (totalAffectedRow < AFFECTED_ONE_ROW) {
            throw new IllegalStateException(errorMsgSupplier.get());
        }
    }

    protected void assertManyAffectedRow(int totalAffectedRow, String errorMsg) {
        if (totalAffectedRow < AFFECTED_ONE_ROW) {
            throw new IllegalStateException(errorMsg);
        }
    }

    public long generateId() {
        return idGenerator.generateId();
    }

    public List<SchedTask> splitTasks(JobHandlerParam param, long instanceId, Date date) throws JobCheckedException {
        if (RouteStrategy.BROADCAST == param.getRouteStrategy()) {
            List<Worker> discoveredServers = workerDiscover.getDiscoveredServers(param.getJobGroup());
            if (discoveredServers.isEmpty()) {
                throw new JobCheckedException(JobCodeMsg.NOT_DISCOVERED_WORKER);
            }
            int count = discoveredServers.size();
            return IntStream.range(0, count)
                .mapToObj(i -> SchedTask.create(param.getJobParam(), generateId(), instanceId, i + 1, count, date, discoveredServers.get(i).serialize()))
                .collect(Collectors.toList());
        } else {
            List<SplitTask> split = workerCoreRpcClient.split(param);
            Assert.notEmpty(split, () -> "Not split any task: " + param);
            Assert.isTrue(split.size() <= MAX_SPLIT_TASK_SIZE, () -> "Split task size must less than " + MAX_SPLIT_TASK_SIZE + ", job=" + param);
            int count = split.size();
            return IntStream.range(0, count)
                .mapToObj(i -> SchedTask.create(split.get(i).getTaskParam(), generateId(), instanceId, i + 1, count, date, null))
                .collect(Collectors.toList());
        }
    }

    public boolean hasAliveExecuting(List<SchedTask> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return false;
        }
        return tasks.stream()
            .filter(e -> ExecuteState.EXECUTING.equals(e.getExecuteState()))
            .map(SchedTask::getWorker)
            .anyMatch(this::isAliveWorker);
    }

    public boolean isAliveWorker(String text) {
        return StringUtils.isNotBlank(text)
            && isAliveWorker(Worker.deserialize(text));
    }

    public boolean isDeadWorker(String text) {
        return !isAliveWorker(text);
    }

    public boolean isAliveWorker(Worker worker) {
        return worker != null && workerDiscover.isDiscoveredServer(worker);
    }

    public boolean isDeadWorker(Worker worker) {
        return !isAliveWorker(worker);
    }

    public boolean hasNotDiscoveredWorkers(String group) {
        return CollectionUtils.isEmpty(workerDiscover.getDiscoveredServers(group));
    }

    public boolean hasNotDiscoveredWorkers() {
        return !workerDiscover.hasDiscoveredServers();
    }

    public boolean dispatch(SchedJob job, SchedInstance instance, List<SchedTask> tasks) {
        ExecuteTaskParam.Builder builder = ExecuteTaskParam.builder(instance, job);
        List<ExecuteTaskParam> list;
        if (RouteStrategy.BROADCAST.equals(job.getRouteStrategy())) {
            list = new ArrayList<>(tasks.size());
            for (SchedTask task : tasks) {
                Assert.hasText(task.getWorker(), () -> "Broadcast route strategy worker must pre assign: " + task.getTaskId());
                Worker worker = Worker.deserialize(task.getWorker());
                if (isDeadWorker(worker)) {
                    cancelWaitingTask(task.getTaskId());
                } else {
                    list.add(builder.build(Operations.TRIGGER, task.getTaskId(), instance.getTriggerTime(), worker));
                }
            }
        } else {
            list = tasks.stream()
                .map(e -> builder.build(Operations.TRIGGER, e.getTaskId(), instance.getTriggerTime(), null))
                .collect(Collectors.toList());
        }

        return taskDispatcher.dispatch(list, job.getJobGroup());
    }

    public boolean dispatch(List<ExecuteTaskParam> params) {
        List<ExecuteTaskParam> list = new ArrayList<>(params.size());
        for (ExecuteTaskParam param : params) {
            if (RouteStrategy.BROADCAST == param.getRouteStrategy() && isDeadWorker(param.getWorker())) {
                cancelWaitingTask(param.getTaskId());
            } else {
                list.add(param);
            }
        }
        return taskDispatcher.dispatch(list);
    }

    /**
     * Broadcast strategy task after assigned worker.
     * if the worker was dead, should cancel the task.
     *
     * @param taskId the task id
     * @return {@code true} if cancel successful
     */
    protected abstract boolean cancelWaitingTask(long taskId);

    // ------------------------------------------------------------------private methods

    private void parseTriggerConfig(SchedJob job, boolean isUpdate) {
        TriggerType triggerType = TriggerType.of(job.getTriggerType());
        Long jobId = job.getJobId();

        if (triggerType == TriggerType.DEPEND) {
            List<Long> parentJobIds = SchedDepend.parseTriggerValue(job.getTriggerValue());
            Assert.notEmpty(parentJobIds, () -> "Invalid dependency parent job id config: " + job.getTriggerValue());
            Assert.isTrue(!parentJobIds.contains(jobId), () -> "Cannot depends self: " + jobId + ", " + parentJobIds);

            Map<Long, SchedJob> parentJobMap = jobMapper.findByJobIds(parentJobIds)
                .stream()
                .collect(Collectors.toMap(SchedJob::getJobId, Function.identity()));
            for (Long parentJobId : parentJobIds) {
                SchedJob parentJob = parentJobMap.get(parentJobId);
                Assert.notNull(parentJob, () -> "Parent job id not found: " + parentJobId);
                if (!job.getJobGroup().equals(parentJob.getJobGroup())) {
                    throw new IllegalArgumentException("Invalid group: parent=" + parentJob.getJobGroup() + ", child=" + job.getJobGroup());
                }
            }

            // 校验是否有循环依赖
            if (isUpdate) {
                checkCircularDepends(jobId, new HashSet<>(parentJobIds));
            }

            List<SchedDepend> list = new ArrayList<>(parentJobIds.size());
            for (int i = 0; i < parentJobIds.size(); i++) {
                list.add(new SchedDepend(parentJobIds.get(i), jobId, i + 1));
            }

            dependMapper.batchInsert(list);
            job.setTriggerValue(Joiner.on(Str.COMMA).join(parentJobIds));
            job.setNextTriggerTime(null);
        } else {
            Date nextTriggerTime;
            if (FIXED_TYPES.contains(triggerType)) {
                nextTriggerTime = Dates.max(new Date(), job.getStartTime());
            } else {
                Date baseTime = Dates.max(new Date(), job.getStartTime());
                nextTriggerTime = triggerType.computeNextTriggerTime(job.getTriggerValue(), baseTime);
            }

            if (nextTriggerTime == null) {
                throw new IllegalArgumentException("Not next trigger time: " + job.getTriggerType() + ", " + job.getTriggerValue());
            }
            if (job.getEndTime() != null && nextTriggerTime.after(job.getEndTime())) {
                throw new IllegalArgumentException("Expire next trigger time: " + job.getTriggerType() + ", " + job.getTriggerValue());
            }
            job.setNextTriggerTime(nextTriggerTime.getTime());
        }
    }

    private void checkCircularDepends(Long jobId, Set<Long> parentJobIds) {
        for (; ; ) {
            Map<Long, SchedDepend> map = dependMapper.findByChildJobIds(parentJobIds)
                .stream()
                .collect(Collectors.toMap(SchedDepend::getParentJobId, Function.identity(), (v1, v2) -> v1));
            if (MapUtils.isEmpty(map)) {
                return;
            }
            if (map.containsKey(jobId)) {
                throw new IllegalArgumentException("Circular depends job: " + map.get(jobId));
            }
            parentJobIds = map.keySet();
        }
    }

}
