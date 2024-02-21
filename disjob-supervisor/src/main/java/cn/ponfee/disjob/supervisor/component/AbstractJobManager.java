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
import cn.ponfee.disjob.common.base.Symbol.Str;
import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.core.base.JobCodeMsg;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.base.WorkerRpcService;
import cn.ponfee.disjob.core.enums.*;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.exception.KeyExistsException;
import cn.ponfee.disjob.core.handle.SplitTask;
import cn.ponfee.disjob.core.model.SchedDepend;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.core.param.worker.ExistsTaskParam;
import cn.ponfee.disjob.core.param.worker.JobHandlerParam;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import cn.ponfee.disjob.dispatch.TaskDispatcher;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.registry.rpc.DestinationServerRestProxy.DestinationServerInvoker;
import cn.ponfee.disjob.registry.rpc.DiscoveryServerRestProxy.GroupedServerInvoker;
import cn.ponfee.disjob.supervisor.application.SchedGroupService;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedDependMapper;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedJobMapper;
import com.google.common.base.Joiner;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static cn.ponfee.disjob.common.spring.TransactionUtils.assertOneAffectedRow;
import static cn.ponfee.disjob.common.spring.TransactionUtils.isOneAffectedRow;
import static cn.ponfee.disjob.supervisor.dao.SupervisorDataSourceConfig.SPRING_BEAN_NAME_TX_MANAGER;

/**
 * Abstract job manager
 *
 * @author Ponfee
 */
@RequiredArgsConstructor
public abstract class AbstractJobManager {
    private static final int MAX_SPLIT_TASK_SIZE = 1000;
    private static final int MAX_DEPENDS_LEVEL = 20;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final SchedJobMapper jobMapper;
    protected final SchedDependMapper dependMapper;

    private final IdGenerator idGenerator;
    private final SupervisorRegistry workerDiscover;
    private final TaskDispatcher taskDispatcher;
    private final GroupedServerInvoker<WorkerRpcService> groupedWorkerRpcClient;
    private final DestinationServerInvoker<WorkerRpcService, Worker> destinationWorkerRpcClient;

    // ------------------------------------------------------------------database single operation without spring transactional

    public boolean disableJob(SchedJob job) {
        return isOneAffectedRow(jobMapper.disable(job));
    }

    public boolean changeJobState(long jobId, JobState to) {
        boolean flag = isOneAffectedRow(jobMapper.updateState(jobId, to.value(), 1 ^ to.value()));
        if (flag && to == JobState.ENABLE) {
            SchedJob job = jobMapper.get(jobId);
            updateFixedDelayNextTriggerTime(job, Dates.ofTimeMillis(job.getLastTriggerTime()));
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

    @Transactional(transactionManager = SPRING_BEAN_NAME_TX_MANAGER, rollbackFor = Exception.class)
    public Long addJob(SchedJob job) throws JobException {
        if (jobMapper.exists(job.getGroup(), job.getJobName())) {
            throw new KeyExistsException("[" + job.getGroup() + "] already exists job name: " + job.getJobName());
        }
        job.setUpdatedBy(job.getCreatedBy());
        job.verifyBeforeAdd();
        job.checkAndDefaultSetting();
        verifyJob(job);
        job.setJobId(generateId());
        parseTriggerConfig(job);

        jobMapper.insert(job);
        return job.getJobId();
    }

    @Transactional(transactionManager = SPRING_BEAN_NAME_TX_MANAGER, rollbackFor = Exception.class)
    public void updateJob(SchedJob job) throws JobException {
        job.verifyBeforeUpdate();
        job.checkAndDefaultSetting();
        if (StringUtils.isEmpty(job.getJobHandler())) {
            Assert.hasText(job.getJobParam(), "Job param must be null if not set job handler.");
        } else {
            verifyJob(job);
        }

        SchedJob dbJob = jobMapper.get(job.getJobId());
        Assert.notNull(dbJob, () -> "Sched job id not found " + job.getJobId());
        Assert.isTrue(dbJob.getGroup().equals(job.getGroup()), "Cannot modify job group.");
        job.setNextTriggerTime(dbJob.getNextTriggerTime());

        if (job.getTriggerType() == null) {
            Assert.isNull(job.getTriggerValue(), "Trigger value must be null if not set trigger type.");
        } else if (!dbJob.equalsTrigger(job.getTriggerType(), job.getTriggerValue())) {
            Assert.notNull(job.getTriggerValue(), "Trigger value cannot be null if has set trigger type.");
            // update last trigger time or depends parent job id
            dependMapper.deleteByChildJobId(job.getJobId());
            parseTriggerConfig(job);
        }

        job.setUpdatedAt(new Date());
        assertOneAffectedRow(jobMapper.update(job), "Update sched job fail or conflict.");
    }

    @Transactional(transactionManager = SPRING_BEAN_NAME_TX_MANAGER, rollbackFor = Exception.class)
    public void deleteJob(long jobId) {
        SchedJob job = jobMapper.get(jobId);
        Assert.notNull(job, "Job id not found: " + jobId);
        if (JobState.ENABLE.equalsValue(job.getJobState())) {
            throw new IllegalStateException("Please disable job before delete this job.");
        }
        assertOneAffectedRow(jobMapper.softDelete(jobId), "Delete sched job fail or conflict.");
        dependMapper.deleteByParentJobId(jobId);
        dependMapper.deleteByChildJobId(jobId);
    }

    // ------------------------------------------------------------------others operation

    protected boolean updateFixedDelayNextTriggerTime(SchedJob job, Date baseTime) {
        TriggerType fixedDelay = TriggerType.FIXED_DELAY;
        if (!fixedDelay.equalsValue(job.getTriggerType())) {
            return false;
        }
        Date date = baseTime == null ? null : fixedDelay.computeNextTriggerTime(job.getTriggerValue(), baseTime);
        Date nextTriggerTime = Dates.max(new Date(), job.getStartTime(), date);
        return isOneAffectedRow(jobMapper.updateFixedDelayNextTriggerTime(job.getJobId(), nextTriggerTime.getTime()));
    }

    public long generateId() {
        return idGenerator.generateId();
    }

    public List<SchedTask> splitTasks(JobHandlerParam param, long instanceId, Date date) throws JobException {
        if (RouteStrategy.BROADCAST == param.getRouteStrategy()) {
            List<Worker> discoveredServers = workerDiscover.getDiscoveredServers(param.getGroup());
            if (discoveredServers.isEmpty()) {
                throw new JobException(JobCodeMsg.NOT_DISCOVERED_WORKER);
            }
            int count = discoveredServers.size();
            return IntStream.range(0, count)
                .mapToObj(i -> SchedTask.create(param.getJobParam(), generateId(), instanceId, i + 1, count, date, discoveredServers.get(i).serialize()))
                .collect(Collectors.toList());
        } else {
            List<SplitTask> split = splitJob(param);
            Assert.notEmpty(split, () -> "Not split any task: " + param);
            Assert.isTrue(split.size() <= MAX_SPLIT_TASK_SIZE, () -> "Split task size must less than " + MAX_SPLIT_TASK_SIZE + ", job=" + param);
            int count = split.size();
            return IntStream.range(0, count)
                .mapToObj(i -> SchedTask.create(Optional.ofNullable(split.get(i)).map(SplitTask::getTaskParam).orElse(null), generateId(), instanceId, i + 1, count, date, null))
                .collect(Collectors.toList());
        }
    }

    public boolean hasAliveExecuting(List<SchedTask> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return false;
        }
        return tasks.stream()
            .filter(e -> ExecuteState.EXECUTING.equalsValue(e.getExecuteState()))
            .map(SchedTask::getWorker)
            .anyMatch(this::isAliveWorker);
    }

    public boolean isAliveWorker(String text) {
        return StringUtils.isNotBlank(text)
            && isAliveWorker(Worker.deserialize(text));
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

    public boolean shouldRedispatch(SchedTask task) {
        if (!ExecuteState.WAITING.equalsValue(task.getExecuteState())) {
            return false;
        }
        if (StringUtils.isBlank(task.getWorker())) {
            // not dispatched to a worker successfully
            return true;
        }

        Worker worker = Worker.deserialize(task.getWorker());
        if (isDeadWorker(worker)) {
            // dispatched worker are dead
            return true;
        }

        String supervisorToken = SchedGroupService.createSupervisorAuthenticationToken(worker.getGroup());
        ExistsTaskParam param = new ExistsTaskParam(supervisorToken, task.getTaskId());
        try {
            // `WorkerRpcService#existsTask`：判断任务是否在线程池中，如果不在则可能是没有分发成功，需要重新分发。
            // 因扫描(WaitingInstanceScanner/RunningInstanceScanner)时间是很滞后的，
            // 所以若任务已分发成功，不考虑该任务还在时间轮中的可能性，认定任务已在线程池(WorkerThreadPool)中了。
            return !destinationWorkerRpcClient.invoke(worker, client -> client.existsTask(param));
        } catch (Throwable e) {
            log.error("Invoke worker exists task error: " + worker, e);
            // 若调用异常(如请求超时)，本次不做处理，等下一次扫描时再判断是否要重新分发任务
            return false;
        }
    }

    public boolean dispatch(SchedJob job, SchedInstance instance, List<SchedTask> tasks) {
        String supervisorToken = SchedGroupService.createSupervisorAuthenticationToken(job.getGroup());
        ExecuteTaskParam.Builder builder = ExecuteTaskParam.builder(instance, job, supervisorToken);
        List<ExecuteTaskParam> list;
        if (RouteStrategy.BROADCAST.equalsValue(job.getRouteStrategy())) {
            list = new ArrayList<>(tasks.size());
            for (SchedTask task : tasks) {
                Worker worker = Worker.deserialize(task.getWorker());
                if (isDeadWorker(worker)) {
                    cancelWaitingTask(task.getTaskId());
                } else {
                    list.add(builder.build(Operation.TRIGGER, task.getTaskId(), instance.getTriggerTime(), worker));
                }
            }
        } else {
            list = tasks.stream()
                .map(e -> builder.build(Operation.TRIGGER, e.getTaskId(), instance.getTriggerTime(), null))
                .collect(Collectors.toList());
        }

        return taskDispatcher.dispatch(job.getGroup(), list);
    }

    public boolean dispatch(List<ExecuteTaskParam> params) {
        return taskDispatcher.dispatch(params);
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

    private void verifyJob(SchedJob job) throws JobException {
        JobHandlerParam param = JobHandlerParam.from(job);
        SchedGroupService.fillSupervisorAuthenticationToken(job.getGroup(), param);
        groupedWorkerRpcClient.invokeWithoutResult(job.getGroup(), client -> client.verify(param));
    }

    private List<SplitTask> splitJob(JobHandlerParam param) throws JobException {
        SchedGroupService.fillSupervisorAuthenticationToken(param.getGroup(), param);
        return groupedWorkerRpcClient.invoke(param.getGroup(), client -> client.split(param));
    }

    private void parseTriggerConfig(SchedJob job) {
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
                if (!job.getGroup().equals(parentJob.getGroup())) {
                    throw new IllegalArgumentException("Invalid group: parent=" + parentJob.getGroup() + ", child=" + job.getGroup());
                }
            }

            // 校验是否有循环依赖 以及 依赖层级是否太深
            checkCircularDepends(jobId, new HashSet<>(parentJobIds));

            List<SchedDepend> list = new ArrayList<>(parentJobIds.size());
            for (int i = 0; i < parentJobIds.size(); i++) {
                list.add(new SchedDepend(parentJobIds.get(i), jobId, i + 1));
            }

            dependMapper.batchInsert(list);
            job.setTriggerValue(Joiner.on(Str.COMMA).join(parentJobIds));
            job.setNextTriggerTime(null);
        } else {
            Date nextTriggerTime;
            if (TriggerType.Const.FIXED_TYPES.contains(triggerType)) {
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
        Set<Long> outerDepends = parentJobIds;
        for (int i = 1; ; i++) {
            Map<Long, SchedDepend> map = dependMapper.findByChildJobIds(parentJobIds)
                .stream()
                .collect(Collectors.toMap(SchedDepend::getParentJobId, Function.identity(), (v1, v2) -> v1));
            if (MapUtils.isEmpty(map)) {
                return;
            }
            if (map.containsKey(jobId)) {
                throw new IllegalArgumentException("Circular depends job: " + map.get(jobId));
            }
            if (i >= MAX_DEPENDS_LEVEL) {
                throw new IllegalArgumentException("Too many depends level: " + outerDepends);
            }
            parentJobIds = map.keySet();
        }
    }

}
