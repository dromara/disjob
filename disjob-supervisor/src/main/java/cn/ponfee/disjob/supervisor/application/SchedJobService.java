/*
 * Copyright 2022-2026 Ponfee (http://www.ponfee.cn/)
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
import cn.ponfee.disjob.core.enums.ExecuteStatus;
import cn.ponfee.disjob.core.enums.JobStatus;
import cn.ponfee.disjob.core.enums.Operation;
import cn.ponfee.disjob.core.exception.JobException;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sched job service.
 *
 * @author Ponfee
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedJobService extends SingletonClassConstraint {

    private final JobManager jobManager;
    private final JobQuerier jobQuerier;

    // ------------------------------------------------------------------job

    public Long addJob(String user, SchedJobAddRequest req) throws JobException {
        log.info("Adding job by {}: {}", user, req);
        return jobManager.addJob(req.tosSchedJob(user));
    }

    public void updateJob(String user, SchedJobUpdateRequest req) throws JobException {
        log.info("Updating job by {}: {}", user, req);
        jobManager.updateJob(req.tosSchedJob(user));
    }

    public void deleteJob(String user, long jobId) {
        log.info("Deleting job by {}: {}", user, jobId);
        jobManager.deleteJob(user, jobId);
    }

    public void changeJobStatus(String user, long jobId, int toJobStatus) {
        JobStatus toStatus = JobStatus.of(toJobStatus);
        log.info("Changing job status by {}: {}, {}", user, jobId, toStatus);
        jobManager.changeJobStatus(user, jobId, toStatus);
    }

    public void manualTriggerJob(String user, long jobId) throws JobException {
        log.info("Manual trigger job by {}: {}", user, jobId);
        jobManager.manualTriggerJob(jobId);
    }

    public SchedJobResponse getJob(long jobId) {
        SchedJob job = jobQuerier.getJob(jobId);
        Assert.notNull(job, () -> "Job not found: " + jobId);
        return SchedJobConverter.INSTANCE.convert(job);
    }

    public PageResponse<SchedJobResponse> queryJobForPage(SchedJobPageRequest pageRequest) {
        if (CollectionUtils.isEmpty(pageRequest.getGroups())) {
            return pageRequest.empty();
        }
        return jobQuerier.queryJobForPage(pageRequest);
    }

    public List<Map<String, Object>> searchJob(SchedJobSearchRequest req) {
        if (CollectionUtils.isEmpty(req.getGroups())) {
            return Collections.emptyList();
        }
        return jobQuerier.searchJob(req);
    }

    // ------------------------------------------------------------------instance

    public void pauseInstance(String user, long instanceId) throws JobException {
        log.info("Pausing instance by {}: {}", user, instanceId);
        if (!jobManager.pauseInstance(instanceId)) {
            throw new JobException(JobCodeMsg.NOT_PAUSABLE_INSTANCE);
        }
    }

    public void cancelInstance(String user, long instanceId) throws JobException {
        log.info("Canceling instance by {}: {}", user, instanceId);
        if (!jobManager.cancelInstance(instanceId, Operation.MANUAL_CANCEL)) {
            throw new JobException(JobCodeMsg.NOT_CANCELABLE_INSTANCE);
        }
    }

    public void resumeInstance(String user, long instanceId) throws JobException {
        log.info("Resuming instance by {}: {}", user, instanceId);
        if (!jobManager.resumeInstance(instanceId)) {
            throw new JobException(JobCodeMsg.NOT_RESUMABLE_INSTANCE);
        }
    }

    public void changeInstanceStatus(String user, long instanceId, int toExecuteStatus) {
        ExecuteStatus toStatus = ExecuteStatus.of(toExecuteStatus);
        log.info("Changing instance status by {}: {}, {}", user, instanceId, toStatus);
        jobManager.changeInstanceStatus(instanceId, toStatus);
    }

    public void deleteInstance(String user, long instanceId) {
        log.info("Deleting instance by {}: {}", user, instanceId);
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
