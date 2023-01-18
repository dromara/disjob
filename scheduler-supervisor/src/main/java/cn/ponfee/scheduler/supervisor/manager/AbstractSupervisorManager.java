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
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.core.model.SchedTrack;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.dispatch.TaskDispatcher;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import cn.ponfee.scheduler.supervisor.base.WorkerServiceClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The basic class of definition supervisor manager
 *
 * @author Ponfee
 */
public abstract class AbstractSupervisorManager {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final IdGenerator idGenerator;
    private final SupervisorRegistry discoveryWorker;
    private final TaskDispatcher taskDispatcher;
    private final WorkerServiceClient workerServiceClient;

    public AbstractSupervisorManager(IdGenerator idGenerator,
                                     SupervisorRegistry discoveryWorker,
                                     TaskDispatcher taskDispatcher,
                                     WorkerServiceClient workerServiceClient) {
        this.idGenerator = idGenerator;
        this.discoveryWorker = discoveryWorker;
        this.taskDispatcher = taskDispatcher;
        this.workerServiceClient = workerServiceClient;
    }

    public long generateId() {
        return idGenerator.generateId();
    }

    public void verifyJobHandler(SchedJob job) {
        Assert.isTrue(StringUtils.isNotEmpty(job.getJobHandler()), "Job handler cannot be empty.");
        boolean result = workerServiceClient.verify(job.getJobGroup(), job.getJobHandler(), job.getJobParam());
        if (!result) {
            throw new IllegalArgumentException("Invalid job handler config: " + job.getJobHandler() + ", " + result);
        }
    }

    public List<SchedTask> splitTasks(SchedJob job, long trackId, Date date) throws JobException {
        List<SplitTask> split = workerServiceClient.split(job.getJobGroup(), job.getJobHandler(), job.getJobParam());
        Assert.notEmpty(split, "Not split any task: " + job);

        return split.stream()
            .map(e -> SchedTask.create(e.getTaskParam(), generateId(), trackId, date))
            .collect(Collectors.toList());
    }

    public List<SchedTask> filterDispatchingTask(List<SchedTask> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return Collections.emptyList();
        }
        return tasks.stream()
            .filter(e -> ExecuteState.WAITING.equals(e.getExecuteState()))
            .filter(e -> !isAliveWorker(e.getWorker()))
            .collect(Collectors.toList());
    }

    public boolean hasAliveExecuting(List<SchedTask> tasks) {
        Assert.notEmpty(tasks, "Task list cannot be empty.");
        return tasks.stream()
            .filter(e -> ExecuteState.EXECUTING.equals(e.getExecuteState()))
            .map(SchedTask::getWorker)
            .anyMatch(this::isAliveWorker);
    }

    public boolean isAliveWorker(String text) {
        return StringUtils.isNotBlank(text)
            && isAliveWorker(Worker.deserialize(text));
    }

    public boolean isAliveWorker(Worker worker) {
        return worker != null
            && discoveryWorker.isDiscoveredServerAlive(worker);
    }

    public boolean hasNotFoundWorkers(String group) {
        return CollectionUtils.isEmpty(discoveryWorker.getDiscoveredServers(group));
    }

    public boolean hasNotFoundWorkers() {
        return CollectionUtils.isEmpty(discoveryWorker.getDiscoveredServers());
    }

    public boolean dispatch(SchedJob job, SchedTrack track, List<SchedTask> tasks) {
        return taskDispatcher.dispatch(job, track, tasks);
    }

    public boolean dispatch(List<ExecuteParam> tasks) {
        return taskDispatcher.dispatch(tasks);
    }

}
