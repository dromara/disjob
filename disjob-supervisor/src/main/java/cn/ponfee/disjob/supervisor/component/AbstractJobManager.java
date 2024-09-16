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
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.tuple.Tuple2;
import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.base.WorkerRpcService;
import cn.ponfee.disjob.core.dto.worker.ExistsTaskParam;
import cn.ponfee.disjob.core.dto.worker.SplitJobParam;
import cn.ponfee.disjob.core.dto.worker.SplitJobResult;
import cn.ponfee.disjob.core.dto.worker.VerifyJobParam;
import cn.ponfee.disjob.core.enums.*;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.model.SchedDepend;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.core.util.DisjobUtils;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import cn.ponfee.disjob.dispatch.TaskDispatcher;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.registry.rpc.DestinationServerRestProxy.DestinationServerInvoker;
import cn.ponfee.disjob.registry.rpc.DiscoveryServerRestProxy.GroupedServerInvoker;
import cn.ponfee.disjob.supervisor.application.SchedGroupService;
import cn.ponfee.disjob.supervisor.configuration.SupervisorProperties;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedDependMapper;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedJobMapper;
import cn.ponfee.disjob.supervisor.exception.KeyExistsException;
import cn.ponfee.disjob.supervisor.util.TriggerTimeUtils;
import com.google.common.base.Joiner;
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
public abstract class AbstractJobManager {

    private static final Comparator<Tuple2<Worker, Long>> WORKLOAD_COMPARATOR = Comparator.comparingLong(e -> e.b);

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final SupervisorProperties conf;
    protected final SchedJobMapper jobMapper;
    protected final SchedDependMapper dependMapper;

    private final IdGenerator idGenerator;
    private final SupervisorRegistry workerDiscover;
    private final TaskDispatcher taskDispatcher;
    private final GroupedServerInvoker<WorkerRpcService> groupedWorkerRpcClient;
    private final DestinationServerInvoker<WorkerRpcService, Worker> destinationWorkerRpcClient;

    protected AbstractJobManager(SupervisorProperties conf,
                                 SchedJobMapper jobMapper,
                                 SchedDependMapper dependMapper,
                                 IdGenerator idGenerator,
                                 SupervisorRegistry workerDiscover,
                                 TaskDispatcher taskDispatcher,
                                 GroupedServerInvoker<WorkerRpcService> groupedWorkerRpcClient,
                                 DestinationServerInvoker<WorkerRpcService, Worker> destinationWorkerRpcClient) {
        conf.check();
        this.conf = conf;
        this.jobMapper = jobMapper;
        this.dependMapper = dependMapper;
        this.idGenerator = idGenerator;
        this.workerDiscover = workerDiscover;
        this.taskDispatcher = taskDispatcher;
        this.groupedWorkerRpcClient = groupedWorkerRpcClient;
        this.destinationWorkerRpcClient = destinationWorkerRpcClient;
    }

    public SchedJob getRequireJob(long jobId) {
        return Objects.requireNonNull(jobMapper.get(jobId), () -> "Not found job: " + jobId);
    }

    // ------------------------------------------------------------------database single operation without spring transactional

    public boolean disableJob(SchedJob job) {
        return isOneAffectedRow(jobMapper.disable(job));
    }

    public boolean updateJobNextTriggerTime(SchedJob job) {
        return isOneAffectedRow(jobMapper.updateNextTriggerTime(job));
    }

    public boolean updateJobNextScanTime(SchedJob job) {
        return isOneAffectedRow(jobMapper.updateNextScanTime(job));
    }

    // ------------------------------------------------------------------database operation within spring @transactional

    @Transactional(transactionManager = SPRING_BEAN_NAME_TX_MANAGER, rollbackFor = Exception.class)
    public boolean changeJobState(long jobId, JobState toState) {
        boolean updated = isOneAffectedRow(jobMapper.updateState(jobId, toState.value(), 1 ^ toState.value()));
        if (updated && toState == JobState.ENABLED) {
            updateNextTriggerTime(jobMapper.get(jobId));
        }
        return updated;
    }

    @Transactional(transactionManager = SPRING_BEAN_NAME_TX_MANAGER, rollbackFor = Exception.class)
    public Long addJob(SchedJob job) throws JobException {
        job.setUpdatedBy(job.getCreatedBy());
        job.verifyForAdd(conf.getMaximumJobRetryCount());
        if (jobMapper.getJobId(job.getGroup(), job.getJobName()) != null) {
            throw new KeyExistsException("Exists job name: " + job.getJobName());
        }
        verifyJobExecutor(job);
        job.setJobId(generateId());
        parseTriggerConfig(job);

        jobMapper.insert(job);
        return job.getJobId();
    }

    @Transactional(transactionManager = SPRING_BEAN_NAME_TX_MANAGER, rollbackFor = Exception.class)
    public void updateJob(SchedJob job) throws JobException {
        job.verifyForUpdate(conf.getMaximumJobRetryCount());

        if (job.requiredUpdateExecutor()) {
            verifyJobExecutor(job);
        }

        Long jobId0 = jobMapper.getJobId(job.getGroup(), job.getJobName());
        if (jobId0 != null && !jobId0.equals(job.getJobId())) {
            throw new IllegalArgumentException("Exists job name: " + job.getJobName());
        }

        SchedJob dbJob = jobMapper.get(job.getJobId());
        Assert.notNull(dbJob, () -> "Sched job id not found " + job.getJobId());
        Assert.isTrue(dbJob.getGroup().equals(job.getGroup()), "Job group cannot be modify.");
        if (job.requiredUpdateTrigger(dbJob.getTriggerType(), dbJob.getTriggerValue())) {
            dependMapper.deleteByChildJobId(job.getJobId());
            parseTriggerConfig(job);
        }

        assertOneAffectedRow(jobMapper.update(job), "Update sched job fail or conflict.");
    }

    @Transactional(transactionManager = SPRING_BEAN_NAME_TX_MANAGER, rollbackFor = Exception.class)
    public void deleteJob(long jobId) {
        SchedJob job = jobMapper.get(jobId);
        Assert.notNull(job, () -> "Job id not found: " + jobId);
        if (job.isEnabled()) {
            throw new IllegalStateException("Please disable job before delete this job.");
        }
        assertOneAffectedRow(jobMapper.softDelete(jobId), "Delete sched job fail or conflict.");
        dependMapper.deleteByParentJobId(jobId);
        dependMapper.deleteByChildJobId(jobId);
    }

    // ------------------------------------------------------------------others operation

    public long generateId() {
        return idGenerator.generateId();
    }

    public List<SchedTask> splitJob(SplitJobParam param, long instanceId) throws JobException {
        if (param.getRouteStrategy().isBroadcast()) {
            List<Worker> workers = workerDiscover.getDiscoveredServers(param.getGroup());
            Assert.state(!workers.isEmpty(), () -> "Not discovered broadcast worker: " + param.getGroup());
            int count = workers.size();
            param.setBroadcastWorkerCount(count);

            List<String> taskParams = splitTasks(param);
            Assert.state(taskParams.size() == count, () -> "Invalid broadcast split size: " + taskParams.size() + "!=" + count);
            return IntStream.range(0, count)
                .mapToObj(i -> SchedTask.of(taskParams.get(i), generateId(), instanceId, i + 1, count, workers.get(i).serialize()))
                .collect(Collectors.toList());
        } else {
            List<String> taskParams = splitTasks(param);
            int count = taskParams.size();
            Assert.state(0 < count && count <= conf.getMaximumSplitTaskSize(), () -> "Invalid basic split size: " + count);
            return IntStream.range(0, count)
                .mapToObj(i -> SchedTask.of(taskParams.get(i), generateId(), instanceId, i + 1, count, null))
                .collect(Collectors.toList());
        }
    }

    public boolean hasAliveExecuting(List<SchedTask> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return false;
        }
        return tasks.stream().filter(SchedTask::isExecuting).anyMatch(e -> isAliveWorker(e.getWorker()));
    }

    public boolean isAliveWorker(String worker) {
        return StringUtils.isNotBlank(worker) && isAliveWorker(Worker.deserialize(worker));
    }

    public boolean isAliveWorker(Worker worker) {
        return worker != null && workerDiscover.isDiscoveredServer(worker);
    }

    public boolean hasNotDiscoveredWorkers(String group) {
        return CollectionUtils.isEmpty(workerDiscover.getDiscoveredServers(group));
    }

    public boolean hasNotDiscoveredWorkers() {
        return !workerDiscover.hasDiscoveredServers();
    }

    public boolean shouldRedispatch(SchedTask task) {
        if (!task.isWaiting()) {
            return false;
        }
        if (StringUtils.isBlank(task.getWorker())) {
            // not dispatched to a worker successfully
            return true;
        }
        Worker worker = Worker.deserialize(task.getWorker());
        if (!isAliveWorker(worker)) {
            // dispatched worker are dead
            return true;
        }

        String supervisorToken = SchedGroupService.createSupervisorAuthenticationToken(worker.getGroup());
        ExistsTaskParam param = ExistsTaskParam.of(supervisorToken, task.getTaskId());
        try {
            // `WorkerRpcService#existsTask`：判断任务是否在线程池中，如果不在则可能是没有分发成功，需要重新分发。
            // 因扫描(WaitingInstanceScanner/RunningInstanceScanner)时间是很滞后的，
            // 所以若任务已分发成功，不考虑该任务还在时间轮中的可能性，认定任务已在线程池(WorkerThreadPool)中了。
            return !destinationWorkerRpcClient.call(worker, client -> client.existsTask(param));
        } catch (Throwable e) {
            log.error("Invoke worker exists task error: " + worker, e);
            // 若调用异常(如请求超时)，本次不做处理，等下一次扫描时再判断是否要重新分发任务
            return false;
        }
    }

    public boolean dispatch(SchedJob job, SchedInstance instance, List<SchedTask> tasks) {
        return dispatch(false, job, instance, tasks);
    }

    public boolean dispatch(boolean redispatch, SchedJob job, SchedInstance instance, List<SchedTask> tasks) {
        ExecuteTaskParam.Builder builder = newExecuteTaskParamBuilder(job, instance);
        RouteStrategy routeStrategy = RouteStrategy.of(job.getRouteStrategy());
        List<ExecuteTaskParam> list = new ArrayList<>(tasks.size());
        List<Tuple2<Worker, Long>> workload;

        if (routeStrategy.isBroadcast()) {
            for (SchedTask task : tasks) {
                Worker worker = Worker.deserialize(task.getWorker());
                if (!isAliveWorker(worker)) {
                    abortBroadcastWaitingTask(task.getTaskId());
                } else {
                    list.add(builder.build(Operation.TRIGGER, task.getTaskId(), instance.getTriggerTime(), worker));
                }
            }
        } else if (!redispatch || routeStrategy.isNotRoundRobin() || (workload = calculateWorkload(job, instance)) == null) {
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

        return taskDispatcher.dispatch(job.getGroup(), list);
    }

    // ------------------------------------------------------------------protected methods

    protected ExecuteTaskParam.Builder newExecuteTaskParamBuilder(SchedJob job, SchedInstance instance) {
        String supervisorToken = SchedGroupService.createSupervisorAuthenticationToken(job.getGroup());
        return ExecuteTaskParam.builder(instance, job, supervisorToken);
    }

    protected boolean dispatch(List<ExecuteTaskParam> tasks) {
        return taskDispatcher.dispatch(tasks);
    }

    /**
     * Broadcast strategy task after assigned worker.
     * if the worker was dead, should cancel the task.
     *
     * @param taskId the task id
     */
    protected abstract void abortBroadcastWaitingTask(long taskId);

    /**
     * Lists the pausable tasks
     *
     * @param instanceId the instance id
     * @return List<SchedTask>
     */
    protected abstract List<SchedTask> listPausableTasks(long instanceId);

    // ------------------------------------------------------------------private methods

    private void updateNextTriggerTime(SchedJob job) {
        if (TriggerType.of(job.getTriggerType()) == TriggerType.DEPEND) {
            return;
        }
        Long nextTriggerTime = computeNextTriggerTime(job);
        if (!nextTriggerTime.equals(job.getNextTriggerTime())) {
            job.setNextTriggerTime(nextTriggerTime);
            assertOneAffectedRow(jobMapper.updateNextTriggerTime(job), () -> "Update next trigger time failed: " + job);
        }
    }

    private void verifyJobExecutor(SchedJob job) throws JobException {
        Assert.hasText(job.getJobExecutor(), () -> "Job executor cannot be blank.");
        DisjobUtils.checkClobMaximumLength(job.getJobExecutor(), "Job executor");
        DisjobUtils.checkClobMaximumLength(job.getJobParam(), "Job param");
        JobType.of(job.getJobType());
        RouteStrategy.of(job.getRouteStrategy());

        VerifyJobParam param = VerifyJobParam.of(job);
        SchedGroupService.fillSupervisorAuthenticationToken(job.getGroup(), param);
        groupedWorkerRpcClient.invoke(job.getGroup(), client -> client.verifyJob(param));
    }

    private List<String> splitTasks(SplitJobParam param) throws JobException {
        SchedGroupService.fillSupervisorAuthenticationToken(param.getGroup(), param);
        SplitJobResult result = groupedWorkerRpcClient.call(param.getGroup(), client -> client.splitJob(param));
        List<String> taskParams = result.getTaskParams();
        taskParams.forEach(e -> DisjobUtils.checkClobMaximumLength(e, "Split task param"));
        return taskParams;
    }

    private void parseTriggerConfig(SchedJob job) {
        String triggerValue = job.getTriggerValue();
        Assert.hasText(triggerValue, "Trigger value cannot be blank.");
        Assert.isTrue(triggerValue.length() <= 255, "Trigger value length cannot exceed 255.");

        Long jobId = job.getJobId();
        if (TriggerType.of(job.getTriggerType()) == TriggerType.DEPEND) {
            List<Long> parentJobIds = SchedDepend.parseTriggerValue(triggerValue);
            Assert.notEmpty(parentJobIds, () -> "Invalid dependency parent job id config: " + triggerValue);
            Assert.isTrue(!parentJobIds.contains(jobId), () -> "Cannot depends self: " + jobId + ", " + parentJobIds);

            Map<Long, SchedJob> parentJobMap = jobMapper.findByJobIds(parentJobIds)
                .stream()
                .collect(Collectors.toMap(SchedJob::getJobId, Function.identity()));
            for (Long parentJobId : parentJobIds) {
                SchedJob parentJob = parentJobMap.get(parentJobId);
                Assert.notNull(parentJob, () -> "Parent job id not found: " + parentJobId);
                if (!job.getGroup().equals(parentJob.getGroup())) {
                    throw new IllegalArgumentException("Inconsistent depend group: " + parentJob.getGroup() + ", " + job.getGroup());
                }
            }

            // 校验是否有循环依赖 以及 依赖层级是否太深
            checkCircularDepends(jobId, new HashSet<>(parentJobIds));

            List<SchedDepend> list = Collects.convert(parentJobIds, pid -> SchedDepend.of(pid, jobId));
            Collects.batchProcess(list, dependMapper::batchInsert, JobConstants.PROCESS_BATCH_SIZE);
            job.setTriggerValue(Joiner.on(Str.COMMA).join(parentJobIds));
            job.setNextTriggerTime(null);
        } else {
            job.setNextTriggerTime(computeNextTriggerTime(job));
        }
    }

    private Long computeNextTriggerTime(SchedJob job) {
        Long lastTriggerTime = job.getLastTriggerTime();
        Date now = new Date();
        job.setLastTriggerTime(Long.max(now.getTime() - 1, lastTriggerTime == null ? 0 : lastTriggerTime));
        Long next = TriggerTimeUtils.computeNextTriggerTime(job, now);
        Assert.notNull(next, () -> "Expire " + TriggerType.of(job.getTriggerType()) + " value: " + job.getTriggerValue());
        job.setLastTriggerTime(lastTriggerTime);
        return next;
    }

    private void checkCircularDepends(Long jobId, Set<Long> parentJobIds) {
        Set<Long> outerDepends = parentJobIds;
        for (int i = 1; ; i++) {
            Map<Long, SchedDepend> map = dependMapper.findByChildJobIds(parentJobIds)
                .stream()
                .collect(Collectors.toMap(SchedDepend::getParentJobId, Function.identity()));
            if (MapUtils.isEmpty(map)) {
                return;
            }
            if (map.containsKey(jobId)) {
                throw new IllegalArgumentException("Circular depends job: " + map.get(jobId));
            }
            if (i >= conf.getMaximumJobDependsDepth()) {
                throw new IllegalArgumentException("Exceed depends depth: " + outerDepends);
            }
            parentJobIds = map.keySet();
        }
    }

    private List<Tuple2<Worker, Long>> calculateWorkload(SchedJob job, SchedInstance instance) {
        List<Worker> workers = workerDiscover.getDiscoveredServers(job.getGroup());
        if (CollectionUtils.isEmpty(workers)) {
            log.error("Not found available worker for calculate workload: {}", job.getGroup());
            return null;
        }
        List<SchedTask> pausableTasks = listPausableTasks(instance.getInstanceId());
        if (CollectionUtils.isEmpty(pausableTasks)) {
            return null;
        }
        Map<String, Long> map = pausableTasks.stream()
            .filter(e -> StringUtils.isNotBlank(e.getWorker()))
            .collect(Collectors.groupingBy(SchedTask::getWorker, Collectors.counting()));
        return workers.stream().map(e -> Tuple2.of(e, map.getOrDefault(e.serialize(), 0L))).collect(Collectors.toList());
    }

}
