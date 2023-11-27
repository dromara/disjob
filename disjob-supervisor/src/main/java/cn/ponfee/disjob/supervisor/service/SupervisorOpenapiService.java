/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_ )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  \___    http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.service;

import cn.ponfee.disjob.common.model.PageResponse;
import cn.ponfee.disjob.core.base.JobCodeMsg;
import cn.ponfee.disjob.core.enums.ExecuteState;
import cn.ponfee.disjob.core.enums.JobState;
import cn.ponfee.disjob.core.enums.Operations;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.exception.JobRuntimeException;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.supervisor.provider.openapi.converter.SchedJobConverter;
import cn.ponfee.disjob.supervisor.provider.openapi.request.AddSchedJobRequest;
import cn.ponfee.disjob.supervisor.provider.openapi.request.SchedInstancePageRequest;
import cn.ponfee.disjob.supervisor.provider.openapi.request.SchedJobPageRequest;
import cn.ponfee.disjob.supervisor.provider.openapi.request.UpdateSchedJobRequest;
import cn.ponfee.disjob.supervisor.provider.openapi.response.SchedInstanceResponse;
import cn.ponfee.disjob.supervisor.provider.openapi.response.SchedJobResponse;
import cn.ponfee.disjob.supervisor.provider.openapi.response.SchedTaskResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Supervisor openapi service.
 *
 * @author Ponfee
 */
@Component
public class SupervisorOpenapiService {

    private static final Logger LOG = LoggerFactory.getLogger(SupervisorOpenapiService.class);

    private final DistributedJobManager jobManager;
    private final DistributedJobQuerier jobQuerier;

    public SupervisorOpenapiService(DistributedJobManager jobManager,
                                    DistributedJobQuerier jobQuerier) {
        this.jobManager = jobManager;
        this.jobQuerier = jobQuerier;
    }

    // ------------------------------------------------------------------ sched job

    public void addJob(AddSchedJobRequest req) throws JobException {
        jobManager.addJob(req.tosSchedJob());
    }

    public void updateJob(UpdateSchedJobRequest req) throws JobException {
        LOG.info("Do updating sched job {}", req.getJobId());
        jobManager.updateJob(req.tosSchedJob());
    }

    public void deleteJob(long jobId) {
        LOG.info("Do deleting sched job {}", jobId);
        jobManager.deleteJob(jobId);
    }

    public Boolean changeJobState(long jobId, int jobState) {
        LOG.info("Do change sched job state {}", jobId);
        return jobManager.changeJobState(jobId, JobState.of(jobState));
    }

    public void triggerJob(long jobId) throws JobException {
        LOG.info("Do manual trigger the sched job {}", jobId);
        jobManager.triggerJob(jobId);
    }

    public SchedJobResponse getJob(long jobId) {
        SchedJob schedJob = jobQuerier.getJob(jobId);
        return SchedJobConverter.INSTANCE.convert(schedJob);
    }

    public PageResponse<SchedJobResponse> queryJobForPage(SchedJobPageRequest pageRequest) {
        return jobQuerier.queryJobForPage(pageRequest);
    }

    // ------------------------------------------------------------------ sched instance

    public void pauseInstance(long instanceId) {
        LOG.info("Do pausing sched instance {}", instanceId);
        if (!jobManager.pauseInstance(instanceId)) {
            throw new JobRuntimeException(JobCodeMsg.NOT_PAUSABLE_INSTANCE);
        }
    }

    public void cancelInstance(long instanceId) {
        LOG.info("Do canceling sched instance {}", instanceId);
        if (!jobManager.cancelInstance(instanceId, Operations.MANUAL_CANCEL)) {
            throw new JobRuntimeException(JobCodeMsg.NOT_CANCELABLE_INSTANCE);
        }
    }

    public void resumeInstance(long instanceId) {
        LOG.info("Do resuming sched instance {}", instanceId);
        if (!jobManager.resumeInstance(instanceId)) {
            throw new JobRuntimeException(JobCodeMsg.NOT_RESUMABLE_INSTANCE);
        }
    }

    public void changeInstanceState(long instanceId, int targetExecuteState) {
        // verify the execution state
        ExecuteState.of(targetExecuteState);

        LOG.info("Do force change state {} | {}", instanceId, targetExecuteState);
        jobManager.changeInstanceState(instanceId, ExecuteState.of(targetExecuteState));
    }

    public void deleteInstance(long instanceId) {
        LOG.info("Do deleting sched instance {}", instanceId);
        jobManager.deleteInstance(instanceId);
    }

    public SchedInstanceResponse getInstance(long instanceId, boolean withTasks) {
        SchedInstance instance = jobQuerier.getInstance(instanceId);
        if (instance == null) {
            return null;
        }

        List<SchedTask> tasks = null;
        if (withTasks) {
            tasks = jobQuerier.findLargeInstanceTasks(instanceId);
        }
        return SchedInstanceResponse.of(instance, tasks);
    }

    public List<SchedTaskResponse> getInstanceTasks(long instanceId) {
        List<SchedTask> tasks = jobQuerier.findLargeInstanceTasks(instanceId);
        if (tasks == null) {
            return null;
        }
        return tasks.stream().map(SchedJobConverter.INSTANCE::convert).collect(Collectors.toList());
    }

    public PageResponse<SchedInstanceResponse> queryInstanceForPage(SchedInstancePageRequest pageRequest) {
        return jobQuerier.queryInstanceForPage(pageRequest);
    }

    public List<SchedInstanceResponse> listInstanceChildren(long pnstanceId) {
        return jobQuerier.listInstanceChildren(pnstanceId);
    }

}
