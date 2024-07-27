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

package cn.ponfee.disjob.supervisor.application;

import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.model.PageResponse;
import cn.ponfee.disjob.core.base.JobCodeMsg;
import cn.ponfee.disjob.core.enums.ExecuteState;
import cn.ponfee.disjob.core.enums.JobState;
import cn.ponfee.disjob.core.enums.Operation;
import cn.ponfee.disjob.core.enums.RunType;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.exception.JobRuntimeException;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.supervisor.application.converter.SchedJobConverter;
import cn.ponfee.disjob.supervisor.application.request.SchedInstancePageRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedJobAddRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedJobPageRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedJobUpdateRequest;
import cn.ponfee.disjob.supervisor.application.response.SchedInstanceResponse;
import cn.ponfee.disjob.supervisor.application.response.SchedJobResponse;
import cn.ponfee.disjob.supervisor.application.response.SchedTaskResponse;
import cn.ponfee.disjob.supervisor.component.DistributedJobManager;
import cn.ponfee.disjob.supervisor.component.DistributedJobQuerier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Supervisor openapi service.
 *
 * @author Ponfee
 */
@Service
public class OpenapiService extends SingletonClassConstraint {

    private static final Logger LOG = LoggerFactory.getLogger(OpenapiService.class);

    private final DistributedJobManager jobManager;
    private final DistributedJobQuerier jobQuerier;

    public OpenapiService(DistributedJobManager jobManager, DistributedJobQuerier jobQuerier) {
        this.jobManager = jobManager;
        this.jobQuerier = jobQuerier;
    }

    // ------------------------------------------------------------------ sched job

    public Long addJob(SchedJobAddRequest req) throws JobException {
        String user = req.getCreatedBy(), group = req.getGroup();
        Set<String> groups = SchedGroupService.myGroups(user);
        Assert.isTrue(groups.contains(group), () -> "User '" + user + "' not has group '" + group + "' permission.");
        return jobManager.addJob(req.tosSchedJob());
    }

    public void updateJob(SchedJobUpdateRequest req) throws JobException {
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

    public void manualTriggerJob(long jobId) throws JobException {
        LOG.info("Do manual trigger the sched job {}", jobId);
        SchedJob job = jobQuerier.getJob(jobId);
        Assert.notNull(job, () -> "Sched job not found: " + jobId);
        jobManager.triggerJob(job, RunType.MANUAL, System.currentTimeMillis());
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
        if (!jobManager.cancelInstance(instanceId, Operation.MANUAL_CANCEL)) {
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

        LOG.info("Do force change state {}, {}", instanceId, targetExecuteState);
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
