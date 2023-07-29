/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.web.controller;

import cn.ponfee.disjob.common.model.Result;
import cn.ponfee.disjob.common.spring.BaseController;
import cn.ponfee.disjob.core.enums.ExecuteState;
import cn.ponfee.disjob.core.enums.JobState;
import cn.ponfee.disjob.core.enums.Operations;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.supervisor.manager.DistributedJobManager;
import cn.ponfee.disjob.supervisor.api.converter.SchedJobConverter;
import cn.ponfee.disjob.supervisor.api.request.AddSchedJobRequest;
import cn.ponfee.disjob.supervisor.api.request.UpdateSchedJobRequest;
import cn.ponfee.disjob.supervisor.api.response.SchedInstanceResponse;
import cn.ponfee.disjob.supervisor.api.response.SchedJobResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Provides to external invoke, for manage the sched job & instance & task
 *
 * @author Ponfee
 */
@Tag(name = "Supervisor open api")
@RestController
@RequestMapping("api")
public class ApiController extends BaseController {
    private static final String DEFAULT_USER = "0";

    private final DistributedJobManager jobManager;

    public ApiController(DistributedJobManager jobManager) {
        this.jobManager = jobManager;
    }

    // ------------------------------------------------------------------ sched job

    @PostMapping("job/add")
    public Result<Void> addJob(@RequestBody AddSchedJobRequest req) throws JobException {
        SchedJob schedJob = req.tosSchedJob();
        schedJob.setCreatedBy(DEFAULT_USER);
        schedJob.setUpdatedBy(DEFAULT_USER);
        jobManager.addJob(schedJob);
        return Result.success();
    }

    @PutMapping("job/update")
    public Result<Void> updateJob(@RequestBody UpdateSchedJobRequest req) throws JobException {
        log.info("Do updating sched job {}", req.getJobId());
        SchedJob schedJob = req.tosSchedJob();
        schedJob.setUpdatedBy(DEFAULT_USER);
        jobManager.updateJob(schedJob);
        return Result.success();
    }

    @DeleteMapping("job/delete")
    public Result<Void> deleteJob(@RequestParam("jobId") long jobId) {
        log.info("Do deleting sched job {}", jobId);
        jobManager.deleteJob(jobId);
        return Result.success();
    }

    @GetMapping("job/get")
    public Result<SchedJobResponse> getJob(@RequestParam("jobId") long jobId) {
        SchedJob schedJob = jobManager.getJob(jobId);
        return Result.success(SchedJobConverter.INSTANCE.convert(schedJob));
    }

    @PostMapping("job/disable")
    public Result<Boolean> disableJob(@RequestParam("jobId") long jobId) {
        log.info("Do disable sched job {}", jobId);
        return Result.success(jobManager.changeJobState(jobId, JobState.DISABLE));
    }

    @PostMapping("job/enable")
    public Result<Boolean> enableJob(@RequestParam("jobId") long jobId) {
        log.info("Do enable sched job {}", jobId);
        return Result.success(jobManager.changeJobState(jobId, JobState.ENABLE));
    }

    @PostMapping("job/trigger")
    public Result<Void> triggerJob(@RequestParam("jobId") long jobId) throws JobException {
        log.info("Do manual trigger the sched job {}", jobId);
        jobManager.triggerJob(jobId);
        return Result.success();
    }

    // ------------------------------------------------------------------ sched instance

    @PostMapping("instance/pause")
    public Result<Boolean> pauseInstance(@RequestParam("instanceId") long instanceId) {
        log.info("Do pausing sched instance {}", instanceId);
        return Result.success(jobManager.pauseInstance(instanceId));
    }

    @PostMapping("instance/cancel")
    public Result<Boolean> cancelInstance(@RequestParam("instanceId") long instanceId) {
        log.info("Do canceling sched instance {}", instanceId);
        boolean res = jobManager.cancelInstance(instanceId, Operations.MANUAL_CANCEL);
        return Result.success(res);
    }

    @PostMapping("instance/resume")
    public Result<Boolean> resumeInstance(@RequestParam("instanceId") long instanceId) {
        log.info("Do resuming sched instance {}", instanceId);
        return Result.success(jobManager.resumeInstance(instanceId));
    }

    @PutMapping("instance/change_state")
    public Result<Void> changeState(@RequestParam("instanceId") long instanceId,
                                    @RequestParam("targetExecuteState") int targetExecuteState) {
        // verify the state
        ExecuteState.of(targetExecuteState);

        log.info("Do force change state {} | {}", instanceId, targetExecuteState);
        jobManager.changeInstanceState(instanceId, ExecuteState.of(targetExecuteState));
        return Result.success();
    }

    @DeleteMapping("instance/delete")
    public Result<Void> deleteInstance(@RequestParam("instanceId") long instanceId) {
        log.info("Do deleting sched instance {}", instanceId);

        jobManager.deleteInstance(instanceId);
        return Result.success();
    }

    @GetMapping("instance/get")
    public Result<SchedInstanceResponse> getInstance(@RequestParam("instanceId") long instanceId) {
        SchedInstance instance = jobManager.getInstance(instanceId);
        if (instance == null) {
            return Result.success(null);
        }

        List<SchedTask> tasks = jobManager.findLargeInstanceTask(instanceId);
        return Result.success(SchedInstanceResponse.of(instance, tasks));
    }

}
