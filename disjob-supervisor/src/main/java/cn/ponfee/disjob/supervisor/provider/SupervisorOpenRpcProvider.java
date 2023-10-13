/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_ )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  \___    http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.provider;

import cn.ponfee.disjob.common.model.PageResponse;
import cn.ponfee.disjob.common.spring.RpcController;
import cn.ponfee.disjob.core.api.supervisor.SupervisorOpenRpcService;
import cn.ponfee.disjob.core.api.supervisor.converter.SchedJobConverter;
import cn.ponfee.disjob.core.api.supervisor.request.AddSchedJobRequest;
import cn.ponfee.disjob.core.api.supervisor.request.SchedInstancePageRequest;
import cn.ponfee.disjob.core.api.supervisor.request.SchedJobPageRequest;
import cn.ponfee.disjob.core.api.supervisor.request.UpdateSchedJobRequest;
import cn.ponfee.disjob.core.api.supervisor.response.SchedInstanceResponse;
import cn.ponfee.disjob.core.api.supervisor.response.SchedJobResponse;
import cn.ponfee.disjob.core.api.supervisor.response.SchedTaskResponse;
import cn.ponfee.disjob.core.base.JobCodeMsg;
import cn.ponfee.disjob.core.enums.ExecuteState;
import cn.ponfee.disjob.core.enums.JobState;
import cn.ponfee.disjob.core.enums.Operations;
import cn.ponfee.disjob.core.exception.JobCheckedException;
import cn.ponfee.disjob.core.exception.JobUncheckedException;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.supervisor.service.DistributedJobManager;
import cn.ponfee.disjob.supervisor.service.DistributedJobQuerier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Supervisor open rpc service provider.
 *
 * @author Ponfee
 */
public class SupervisorOpenRpcProvider implements SupervisorOpenRpcService, RpcController {

    protected static final Logger LOG = LoggerFactory.getLogger(SupervisorOpenRpcProvider.class);

    private final DistributedJobManager jobManager;
    private final DistributedJobQuerier jobQuerier;

    public SupervisorOpenRpcProvider(DistributedJobManager jobManager,
                                     DistributedJobQuerier jobQuerier) {
        this.jobManager = jobManager;
        this.jobQuerier = jobQuerier;
    }

    // ------------------------------------------------------------------ sched job

    @Override
    public void addJob(AddSchedJobRequest req) throws JobCheckedException {
        jobManager.addJob(req.tosSchedJob());
    }

    @Override
    public void updateJob(UpdateSchedJobRequest req) throws JobCheckedException {
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
        return jobManager.changeJobState(jobId, JobState.of(jobState));
    }

    @Override
    public void triggerJob(long jobId) throws JobCheckedException {
        LOG.info("Do manual trigger the sched job {}", jobId);
        jobManager.triggerJob(jobId);
    }

    @Override
    public SchedJobResponse getJob(long jobId) {
        SchedJob schedJob = jobQuerier.getJob(jobId);
        return SchedJobConverter.INSTANCE.convert(schedJob);
    }

    @Override
    public PageResponse<SchedJobResponse> queryJobForPage(SchedJobPageRequest pageRequest) {
        return jobQuerier.queryJobForPage(pageRequest);
    }

    // ------------------------------------------------------------------ sched instance

    @Override
    public void pauseInstance(long instanceId) {
        LOG.info("Do pausing sched instance {}", instanceId);
        if (!jobManager.pauseInstance(instanceId)) {
            throw new JobUncheckedException(JobCodeMsg.NOT_PAUSABLE_INSTANCE);
        }
    }

    @Override
    public void cancelInstance(long instanceId) {
        LOG.info("Do canceling sched instance {}", instanceId);
        if (!jobManager.cancelInstance(instanceId, Operations.MANUAL_CANCEL)) {
            throw new JobUncheckedException(JobCodeMsg.NOT_CANCELABLE_INSTANCE);
        }
    }

    @Override
    public void resumeInstance(long instanceId) {
        LOG.info("Do resuming sched instance {}", instanceId);
        if (!jobManager.resumeInstance(instanceId)) {
            throw new JobUncheckedException(JobCodeMsg.NOT_RESUMABLE_INSTANCE);
        }
    }

    @Override
    public void changeInstanceState(long instanceId, int targetExecuteState) {
        // verify the execution state
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

    @Override
    public List<SchedTaskResponse> getInstanceTasks(long instanceId) {
        List<SchedTask> tasks = jobQuerier.findLargeInstanceTasks(instanceId);
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
        return jobQuerier.listInstanceChildren(pnstanceId);
    }

}
