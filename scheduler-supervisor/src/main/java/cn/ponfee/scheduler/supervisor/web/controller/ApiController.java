/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.web.controller;

import cn.ponfee.scheduler.common.model.Result;
import cn.ponfee.scheduler.common.spring.BaseController;
import cn.ponfee.scheduler.core.enums.ExecuteState;
import cn.ponfee.scheduler.core.enums.JobState;
import cn.ponfee.scheduler.core.enums.Operations;
import cn.ponfee.scheduler.core.exception.JobException;
import cn.ponfee.scheduler.core.model.SchedInstance;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.supervisor.manager.SchedulerJobManager;
import cn.ponfee.scheduler.supervisor.web.converter.SchedJobConverter;
import cn.ponfee.scheduler.supervisor.web.request.AddSchedJobRequest;
import cn.ponfee.scheduler.supervisor.web.request.UpdateSchedJobRequest;
import cn.ponfee.scheduler.supervisor.web.response.SchedInstanceResponse;
import cn.ponfee.scheduler.supervisor.web.response.SchedJobResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Provides to external invoke, for manage the sched job & instance & task
 *
 * @author Ponfee
 */
@Tag(name = "Supervisor openapi", description = "cn.ponfee.scheduler.supervisor.web.controller.ApiController")
@RestController
@RequestMapping("api")
public class ApiController extends BaseController {
    private static final String DEFAULT_USER = "0";

    private final SchedulerJobManager schedulerJobManager;

    public ApiController(SchedulerJobManager schedulerJobManager) {
        this.schedulerJobManager = schedulerJobManager;
    }

    // ------------------------------------------------------------------ sched job

    @PostMapping("job/add")
    public Result<Void> addJob(@RequestBody AddSchedJobRequest req) {
        SchedJob schedJob = req.tosSchedJob();
        schedJob.setCreatedBy(DEFAULT_USER);
        schedJob.setUpdatedBy(DEFAULT_USER);
        schedulerJobManager.addJob(schedJob);
        return Result.success();
    }

    @PutMapping("job/update")
    public Result<Void> updateJob(@RequestBody UpdateSchedJobRequest req) {
        log.info("Do updating sched job {}", req.getJobId());
        SchedJob schedJob = req.tosSchedJob();
        schedJob.setUpdatedBy(DEFAULT_USER);
        schedulerJobManager.updateJob(schedJob);
        return Result.success();
    }

    @DeleteMapping("job/delete")
    public Result<Void> deleteJob(@RequestParam("jobId") long jobId) {
        log.info("Do deleting sched job {}", jobId);
        schedulerJobManager.deleteJob(jobId);
        return Result.success();
    }

    @GetMapping("job/get")
    public Result<SchedJobResponse> getJob(@RequestParam("jobId") long jobId) {
        SchedJob schedJob = schedulerJobManager.getJob(jobId);
        return Result.success(SchedJobConverter.INSTANCE.convert(schedJob));
    }

    @PostMapping("job/disable")
    public Result<Boolean> disableJob(@RequestParam("jobId") long jobId) {
        log.info("Do disable sched job {}", jobId);
        return Result.success(schedulerJobManager.changeJobState(jobId, JobState.DISABLE));
    }

    @PostMapping("job/enable")
    public Result<Boolean> enableJob(@RequestParam("jobId") long jobId) {
        log.info("Do enable sched job {}", jobId);
        return Result.success(schedulerJobManager.changeJobState(jobId, JobState.ENABLE));
    }

    @PostMapping("job/trigger")
    public Result<Void> triggerJob(@RequestParam("jobId") long jobId) throws JobException {
        log.info("Do manual trigger the sched job {}", jobId);
        schedulerJobManager.triggerJob(jobId);
        return Result.success();
    }

    // ------------------------------------------------------------------ sched instance

    @PostMapping("instance/pause")
    public Result<Boolean> pauseInstance(@RequestParam("instanceId") long instanceId) {
        log.info("Do pausing sched instance {}", instanceId);
        Long workflowInstanceId = schedulerJobManager.getWorkflowInstanceId(instanceId);
        return Result.success(schedulerJobManager.pauseInstance(instanceId, workflowInstanceId));
    }

    @PostMapping("instance/cancel")
    public Result<Boolean> cancelInstance(@RequestParam("instanceId") long instanceId) {
        log.info("Do canceling sched instance {}", instanceId);
        Long workflowInstanceId = schedulerJobManager.getWorkflowInstanceId(instanceId);
        return Result.success(schedulerJobManager.cancelInstance(instanceId, workflowInstanceId, Operations.MANUAL_CANCEL));
    }

    @PostMapping("instance/resume")
    public Result<Boolean> resumeInstance(@RequestParam("instanceId") long instanceId) {
        log.info("Do resuming sched instance {}", instanceId);
        Long workflowInstanceId = schedulerJobManager.getWorkflowInstanceId(instanceId);
        return Result.success(schedulerJobManager.resumeInstance(instanceId, workflowInstanceId));
    }

    @PutMapping("instance/force_change_state")
    public Result<Void> forceChangeState(@RequestParam("instanceId") long instanceId,
                                         @RequestParam("targetExecuteState") int targetExecuteState) {
        // verify the state
        ExecuteState.of(targetExecuteState);

        log.info("Do force change state {} | {}", instanceId, targetExecuteState);
        Long workflowInstanceId = schedulerJobManager.getWorkflowInstanceId(instanceId);
        schedulerJobManager.forceChangeState(instanceId, workflowInstanceId, targetExecuteState);
        return Result.success();
    }

    @DeleteMapping("instance/delete")
    public Result<Void> deleteInstance(@RequestParam("instanceId") long instanceId) {
        log.info("Do deleting sched instance {}", instanceId);

        Long workflowInstanceId = schedulerJobManager.getWorkflowInstanceId(instanceId);
        schedulerJobManager.deleteInstance(instanceId, workflowInstanceId);
        return Result.success();
    }

    @GetMapping("instance/get")
    public Result<SchedInstanceResponse> getInstance(@RequestParam("instanceId") long instanceId) {
        SchedInstance instance = schedulerJobManager.getInstance(instanceId);
        if (instance == null) {
            return Result.success(null);
        }

        List<SchedTask> tasks = schedulerJobManager.findLargeTaskByInstanceId(instanceId);
        return Result.success(SchedInstanceResponse.of(instance, tasks));
    }

}
