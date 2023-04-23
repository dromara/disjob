/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.manager;

import cn.ponfee.scheduler.common.base.IdGenerator;
import cn.ponfee.scheduler.core.base.JobCodeMsg;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.enums.ExecuteState;
import cn.ponfee.scheduler.core.enums.Operations;
import cn.ponfee.scheduler.core.enums.RouteStrategy;
import cn.ponfee.scheduler.core.exception.JobException;
import cn.ponfee.scheduler.core.handle.SplitTask;
import cn.ponfee.scheduler.core.model.SchedInstance;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.core.param.ExecuteTaskParam;
import cn.ponfee.scheduler.core.param.ExecuteTaskParamBuilder;
import cn.ponfee.scheduler.dispatch.TaskDispatcher;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import cn.ponfee.scheduler.supervisor.base.WorkerServiceClient;
import cn.ponfee.scheduler.supervisor.param.SplitJobParam;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The base job manager
 *
 * @author Ponfee
 */
@RequiredArgsConstructor
public abstract class AbstractJobManager {

    private static final int MAX_SPLIT_TASK_SIZE = 10000;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final IdGenerator idGenerator;
    private final SupervisorRegistry discoveryWorker;
    private final TaskDispatcher taskDispatcher;
    private final WorkerServiceClient workerServiceClient;

    public long generateId() {
        return idGenerator.generateId();
    }

    public void verifyJob(SchedJob job) {
        Assert.hasText(job.getJobHandler(), "Job handler cannot be empty.");
        boolean result = workerServiceClient.verify(job.getJobGroup(), job.getJobHandler(), job.getJobParam());
        Assert.isTrue(result, () -> "Invalid job: " + job.getJobHandler());
    }

    public List<SchedTask> splitTasks(SplitJobParam param, long instanceId, Date date) throws JobException {

        if (RouteStrategy.BROADCAST.equals(param.getRouteStrategy())) {
            List<Worker> discoveredServers = discoveryWorker.getDiscoveredServers(param.getJobGroup());
            if (discoveredServers.isEmpty()) {
                throw new JobException(JobCodeMsg.NOT_DISCOVERED_WORKER);
            }
            int count = discoveredServers.size();
            return IntStream.range(0, count)
                .mapToObj(i -> SchedTask.create(param.getJobParam(), generateId(), instanceId, i + 1, count, date, discoveredServers.get(i).serialize()))
                .collect(Collectors.toList());
        } else {
            List<SplitTask> split = workerServiceClient.split(param.getJobGroup(), param.getJobHandler(), param.getJobParam());
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
        return worker != null
            && discoveryWorker.isDiscoveredServer(worker);
    }

    public boolean isDeadWorker(Worker worker) {
        return !isAliveWorker(worker);
    }

    public boolean hasNotDiscoveredWorkers(String group) {
        return CollectionUtils.isEmpty(discoveryWorker.getDiscoveredServers(group));
    }

    public boolean hasNotDiscoveredWorkers() {
        return !discoveryWorker.hasDiscoveredServers();
    }

    public boolean dispatch(SchedJob job, SchedInstance instance, List<SchedTask> tasks) {
        boolean isBroadcast = RouteStrategy.BROADCAST.equals(job.getRouteStrategy());
        ExecuteTaskParamBuilder builder = ExecuteTaskParam.builder(instance, job);
        List<ExecuteTaskParam> list = new ArrayList<>(tasks.size());
        for (SchedTask task : tasks) {
            if (isBroadcast) {
                Assert.hasText(task.getWorker(), () -> "Broadcast route strategy worker must pre assign: " + task.getTaskId());
                Worker worker = Worker.deserialize(task.getWorker());
                if (isDeadWorker(worker)) {
                    cancelWaitingTask(task.getTaskId());
                } else {
                    list.add(builder.build(Operations.TRIGGER, task.getTaskId(), instance.getTriggerTime(), worker));
                }
            } else {
                list.add(builder.build(Operations.TRIGGER, task.getTaskId(), instance.getTriggerTime(), null));
            }
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
     * Broadcast strategy after assigned worker, and then the worker is dead,
     * the task always waiting state until canceled.
     *
     * @param taskId the task id
     * @return {@code true} if cancel successful
     */
    protected abstract boolean cancelWaitingTask(long taskId);
}
