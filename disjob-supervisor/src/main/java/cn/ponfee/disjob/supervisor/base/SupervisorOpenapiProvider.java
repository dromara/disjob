/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_ )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  \___    http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.base;

import cn.ponfee.disjob.common.model.PageResponse;
import cn.ponfee.disjob.common.spring.RpcController;
import cn.ponfee.disjob.core.enums.ExecuteState;
import cn.ponfee.disjob.core.enums.JobState;
import cn.ponfee.disjob.core.enums.Operations;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.core.openapi.supervisor.SupervisorOpenapi;
import cn.ponfee.disjob.core.openapi.supervisor.converter.SchedJobConverter;
import cn.ponfee.disjob.core.openapi.supervisor.request.AddSchedJobRequest;
import cn.ponfee.disjob.core.openapi.supervisor.request.SchedInstancePageRequest;
import cn.ponfee.disjob.core.openapi.supervisor.request.SchedJobPageRequest;
import cn.ponfee.disjob.core.openapi.supervisor.request.UpdateSchedJobRequest;
import cn.ponfee.disjob.core.openapi.supervisor.response.SchedInstanceResponse;
import cn.ponfee.disjob.core.openapi.supervisor.response.SchedJobResponse;
import cn.ponfee.disjob.core.openapi.supervisor.response.SchedTaskResponse;
import cn.ponfee.disjob.supervisor.manager.DistributedJobManager;
import cn.ponfee.disjob.supervisor.manager.DistributedJobQuerier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Supervisor open api provider
 *
 * @author Ponfee
 */
public class SupervisorOpenapiProvider implements SupervisorOpenapi, RpcController {

    protected static final Logger LOG = LoggerFactory.getLogger(SupervisorOpenapiProvider.class);

    private final DistributedJobManager jobManager;
    private final DistributedJobQuerier jobQuerier;

    public SupervisorOpenapiProvider(DistributedJobManager jobManager,
                                     DistributedJobQuerier jobQuerier) {
        this.jobManager = jobManager;
        this.jobQuerier = jobQuerier;
    }

    // ------------------------------------------------------------------ sched job

    @Override
    public void addJob(AddSchedJobRequest req) throws JobException {
        jobManager.addJob(req.tosSchedJob());
    }

    @Override
    public void updateJob(UpdateSchedJobRequest req) throws JobException {
        LOG.info("Do updating sched job {}", req.getJobId());
        jobManager.updateJob(req.tosSchedJob());
    }

    @Override
    public void deleteJob(long jobId) {
        LOG.info("Do deleting sched job {}", jobId);
        jobManager.deleteJob(jobId);
    }

    @Override
    public Boolean changeJobState(long jobId, int jobState) {
        LOG.info("Do change sched job state {}", jobId);
        return jobManager.changeJobState(jobId, JobState.DISABLE);
    }

    @Override
    public void triggerJob(long jobId) throws JobException {
        LOG.info("Do manual trigger the sched job {}", jobId);
        jobManager.triggerJob(jobId);
    }

    @Override
    public SchedJobResponse getJob(long jobId) {
        SchedJob schedJob = jobManager.getJob(jobId);
        return SchedJobConverter.INSTANCE.convert(schedJob);
    }

    @Override
    public PageResponse<SchedJobResponse> queryJobForPage(SchedJobPageRequest pageRequest) {
        return jobQuerier.queryJobForPage(pageRequest);
    }

    // ------------------------------------------------------------------ sched instance

    @Override
    public Boolean pauseInstance(long instanceId) {
        LOG.info("Do pausing sched instance {}", instanceId);
        return jobManager.pauseInstance(instanceId);
    }

    @Override
    public Boolean cancelInstance(long instanceId) {
        LOG.info("Do canceling sched instance {}", instanceId);
        return jobManager.cancelInstance(instanceId, Operations.MANUAL_CANCEL);
    }

    @Override
    public Boolean resumeInstance(long instanceId) {
        LOG.info("Do resuming sched instance {}", instanceId);
        return jobManager.resumeInstance(instanceId);
    }

    @Override
    public void changeInstanceState(long instanceId, int targetExecuteState) {
        // verify the state
        ExecuteState.of(targetExecuteState);

        LOG.info("Do force change state {} | {}", instanceId, targetExecuteState);
        jobManager.changeInstanceState(instanceId, ExecuteState.of(targetExecuteState));
    }

    @Override
    public void deleteInstance(long instanceId) {
        LOG.info("Do deleting sched instance {}", instanceId);
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
    public List<SchedTaskResponse> getInstanceTasks(long instanceId) {
        List<SchedTask> tasks = jobManager.findLargeInstanceTask(instanceId);
        if (tasks == null) {
            return null;
        }
        return tasks.stream().map(SchedJobConverter.INSTANCE::convert).collect(Collectors.toList());
    }

    @Override
    public PageResponse<SchedInstanceResponse> queryInstanceForPage(SchedInstancePageRequest pageRequest) {
        return jobQuerier.queryInstanceForPage(pageRequest);
    }

    @Override
    public List<SchedInstanceResponse> listInstanceChildren(long pnstanceId) {
        return jobQuerier.listChildren(pnstanceId);
    }

}
