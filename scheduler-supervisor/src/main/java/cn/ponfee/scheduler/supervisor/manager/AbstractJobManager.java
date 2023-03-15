/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.manager;

import cn.ponfee.scheduler.common.base.IdGenerator;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.enums.ExecuteState;
import cn.ponfee.scheduler.core.exception.JobException;
import cn.ponfee.scheduler.core.handle.SplitTask;
import cn.ponfee.scheduler.core.model.SchedInstance;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.dispatch.TaskDispatcher;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import cn.ponfee.scheduler.supervisor.base.WorkerServiceClient;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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
        Assert.isTrue(StringUtils.isNotEmpty(job.getJobHandler()), "Job handler cannot be empty.");
        boolean result = workerServiceClient.verify(job.getJobGroup(), job.getJobHandler(), job.getJobParam());
        Assert.isTrue(result, () -> "Invalid job: " + job.getJobHandler());
    }

    public List<SchedTask> splitTasks(SchedJob job, long instanceId, Date date) throws JobException {
        List<SplitTask> split = workerServiceClient.split(job.getJobGroup(), job.getJobHandler(), job.getJobParam());
        Assert.notEmpty(split, () -> "Not split any task: " + job);
        Assert.isTrue(split.size() <= MAX_SPLIT_TASK_SIZE, () -> "Split task size must less than " + MAX_SPLIT_TASK_SIZE + ", job=" + job);

        return split.stream()
            .map(e -> SchedTask.create(e.getTaskParam(), generateId(), instanceId, date))
            .collect(Collectors.toList());
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

    public boolean isDeathWorker(String text) {
        return !isAliveWorker(text);
    }

    public boolean isAliveWorker(Worker worker) {
        return worker != null
            && discoveryWorker.isDiscoveredServer(worker);
    }

    public boolean isDeathWorker(Worker worker) {
        return !isAliveWorker(worker);
    }

    public boolean hasNotDiscoveredWorkers(String group) {
        return CollectionUtils.isEmpty(discoveryWorker.getDiscoveredServers(group));
    }

    public boolean hasNotDiscoveredWorkers() {
        return !discoveryWorker.hasDiscoveredServers();
    }

    public boolean dispatch(SchedJob job, SchedInstance instance, List<SchedTask> tasks) {
        return taskDispatcher.dispatch(job, instance, tasks);
    }

    public boolean dispatch(List<ExecuteParam> tasks) {
        return taskDispatcher.dispatch(tasks);
    }

}
