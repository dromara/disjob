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
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.exception.JobRuntimeException;
import cn.ponfee.disjob.supervisor.application.converter.SchedJobConverter;
import cn.ponfee.disjob.supervisor.application.request.*;
import cn.ponfee.disjob.supervisor.application.response.SchedInstanceResponse;
import cn.ponfee.disjob.supervisor.application.response.SchedJobResponse;
import cn.ponfee.disjob.supervisor.application.response.SchedTaskResponse;
import cn.ponfee.disjob.supervisor.component.JobManager;
import cn.ponfee.disjob.supervisor.component.JobQuerier;
import cn.ponfee.disjob.supervisor.model.SchedInstance;
import cn.ponfee.disjob.supervisor.model.SchedJob;
import cn.ponfee.disjob.supervisor.model.SchedTask;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sched job service.
 *
 * @author Ponfee
 */
@Service
@RequiredArgsConstructor
public class SchedJobService extends SingletonClassConstraint {

    private static final Logger LOG = LoggerFactory.getLogger(SchedJobService.class);

    private final JobManager jobManager;
    private final JobQuerier jobQuerier;

    // ------------------------------------------------------------------job

    public Long addJob(String user, SchedJobAddRequest req) throws JobException {
        LOG.info("Adding job by {}: {}", user, req);
        return jobManager.addJob(req.tosSchedJob(user));
    }

    public void updateJob(String user, SchedJobUpdateRequest req) throws JobException {
        LOG.info("Updating job by {}: {}", user, req);
        jobManager.updateJob(req.tosSchedJob(user));
    }

    public void deleteJob(String user, long jobId) {
        LOG.info("Deleting job by {}: {}", user, jobId);
        jobManager.deleteJob(user, jobId);
    }

    public void changeJobState(String user, long jobId, int toJobState) {
        JobState toState = JobState.of(toJobState);
        LOG.info("Changing job state by {}: {}, {}", user, jobId, toState);
        jobManager.changeJobState(user, jobId, toState);
    }

    public void manualTriggerJob(String user, long jobId) throws JobException {
        LOG.info("Manual trigger job by {}: {}", user, jobId);
        jobManager.manualTriggerJob(jobId);
    }

    public SchedJobResponse getJob(long jobId) {
        SchedJob job = jobQuerier.getJob(jobId);
        Assert.notNull(job, () -> "Job not found: " + jobId);
        return SchedJobConverter.INSTANCE.convert(job);
    }

    public PageResponse<SchedJobResponse> queryJobForPage(SchedJobPageRequest pageRequest) {
        return jobQuerier.queryJobForPage(pageRequest);
    }

    public List<Map<String, Object>> searchJob(SchedJobSearchRequest req) {
        return jobQuerier.searchJob(req);
    }

    // ------------------------------------------------------------------instance

    public void pauseInstance(String user, long instanceId) {
        LOG.info("Pausing instance by {}: {}", user, instanceId);
        if (!jobManager.pauseInstance(instanceId)) {
            throw new JobRuntimeException(JobCodeMsg.NOT_PAUSABLE_INSTANCE);
        }
    }

    public void cancelInstance(String user, long instanceId) {
        LOG.info("Canceling instance by {}: {}", user, instanceId);
        if (!jobManager.cancelInstance(instanceId, Operation.MANUAL_CANCEL)) {
            throw new JobRuntimeException(JobCodeMsg.NOT_CANCELABLE_INSTANCE);
        }
    }

    public void resumeInstance(String user, long instanceId) {
        LOG.info("Resuming instance by {}: {}", user, instanceId);
        if (!jobManager.resumeInstance(instanceId)) {
            throw new JobRuntimeException(JobCodeMsg.NOT_RESUMABLE_INSTANCE);
        }
    }

    public void changeInstanceState(String user, long instanceId, int toExecuteState) {
        ExecuteState toState = ExecuteState.of(toExecuteState);
        LOG.info("Changing instance state by {}: {}, {}", user, instanceId, toState);
        jobManager.changeInstanceState(instanceId, toState);
    }

    public void deleteInstance(String user, long instanceId) {
        LOG.info("Deleting instance by {}: {}", user, instanceId);
        jobManager.deleteInstance(instanceId);
    }

    public SchedInstanceResponse getInstance(long instanceId, boolean includeTasks) {
        SchedInstance instance = jobQuerier.getInstance(instanceId);
        Assert.notNull(instance, () -> "Instance not found: " + instanceId);

        List<SchedTask> tasks = includeTasks ? jobQuerier.findLargeInstanceTasks(instanceId) : null;
        return SchedInstanceResponse.of(instance, tasks);
    }

    public List<SchedTaskResponse> getInstanceTasks(long instanceId) {
        List<SchedTask> tasks = jobQuerier.findLargeInstanceTasks(instanceId);
        return tasks.stream().map(SchedJobConverter.INSTANCE::convert).collect(Collectors.toList());
    }

    public PageResponse<SchedInstanceResponse> queryInstanceForPage(SchedInstancePageRequest req) {
        return jobQuerier.queryInstanceForPage(req);
    }

    public List<SchedInstanceResponse> listInstanceChildren(long pnstanceId) {
        return jobQuerier.listInstanceChildren(pnstanceId);
    }

}
