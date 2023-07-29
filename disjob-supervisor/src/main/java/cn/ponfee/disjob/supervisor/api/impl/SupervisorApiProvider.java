/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_ )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  \___    http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.api.impl;

import cn.ponfee.disjob.common.spring.BaseController;
import cn.ponfee.disjob.common.spring.RpcController;
import cn.ponfee.disjob.core.enums.ExecuteState;
import cn.ponfee.disjob.core.enums.JobState;
import cn.ponfee.disjob.core.enums.Operations;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.supervisor.api.SupervisorApi;
import cn.ponfee.disjob.supervisor.api.converter.SchedJobConverter;
import cn.ponfee.disjob.supervisor.api.request.AddSchedJobRequest;
import cn.ponfee.disjob.supervisor.api.request.UpdateSchedJobRequest;
import cn.ponfee.disjob.supervisor.api.response.SchedInstanceResponse;
import cn.ponfee.disjob.supervisor.api.response.SchedJobResponse;
import cn.ponfee.disjob.supervisor.api.response.SchedTaskResponse;
import cn.ponfee.disjob.supervisor.manager.DistributedJobManager;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Supervisor api provider
 *
 * @author Ponfee
 */
public class SupervisorApiProvider extends BaseController implements SupervisorApi, RpcController {

    private final DistributedJobManager jobManager;

    public SupervisorApiProvider(DistributedJobManager jobManager) {
        this.jobManager = jobManager;
    }

    // ------------------------------------------------------------------ sched job

    @Override
    public void addJob(AddSchedJobRequest req) throws JobException {
        SchedJob schedJob = req.tosSchedJob();
        jobManager.addJob(schedJob);
    }

    @Override
    public void updateJob(UpdateSchedJobRequest req) throws JobException {
        log.info("Do updating sched job {}", req.getJobId());
        SchedJob schedJob = req.tosSchedJob();
        jobManager.updateJob(schedJob);
    }

    @Override
    public void deleteJob(long jobId) {
        log.info("Do deleting sched job {}", jobId);
        jobManager.deleteJob(jobId);
    }

    @Override
    public SchedJobResponse getJob(long jobId) {
        SchedJob schedJob = jobManager.getJob(jobId);
        return SchedJobConverter.INSTANCE.convert(schedJob);
    }

    @Override
    public Boolean changeJobState(long jobId, int jobState) {
        log.info("Do change sched job state {}", jobId);
        return jobManager.changeJobState(jobId, JobState.DISABLE);
    }

    @Override
    public void triggerJob(long jobId) throws JobException {
        log.info("Do manual trigger the sched job {}", jobId);
        jobManager.triggerJob(jobId);
    }

    // ------------------------------------------------------------------ sched instance

    @Override
    public Boolean pauseInstance(long instanceId) {
        log.info("Do pausing sched instance {}", instanceId);
        return jobManager.pauseInstance(instanceId);
    }

    @Override
    public Boolean cancelInstance(long instanceId) {
        log.info("Do canceling sched instance {}", instanceId);
        return jobManager.cancelInstance(instanceId, Operations.MANUAL_CANCEL);
    }

    @Override
    public Boolean resumeInstance(long instanceId) {
        log.info("Do resuming sched instance {}", instanceId);
        return jobManager.resumeInstance(instanceId);
    }

    @Override
    public void changeState(long instanceId, int targetExecuteState) {
        // verify the state
        ExecuteState.of(targetExecuteState);

        log.info("Do force change state {} | {}", instanceId, targetExecuteState);
        jobManager.changeInstanceState(instanceId, ExecuteState.of(targetExecuteState));
    }

    @Override
    public void deleteInstance(long instanceId) {
        log.info("Do deleting sched instance {}", instanceId);

        jobManager.deleteInstance(instanceId);
    }

    @Override
    public SchedInstanceResponse getInstance(long instanceId) {
        SchedInstance instance = jobManager.getInstance(instanceId);
        if (instance == null) {
            return null;
        }

        List<SchedTask> tasks = jobManager.findLargeInstanceTask(instanceId);
        return SchedInstanceResponse.of(instance, tasks);
    }

    @Override
    public List<SchedTaskResponse> getTasks(long instanceId) {
        List<SchedTask> tasks = jobManager.findLargeInstanceTask(instanceId);
        if (tasks == null) {
            return null;
        }

        return tasks.stream().map(SchedJobConverter.INSTANCE::convert).collect(Collectors.toList());
    }

}
