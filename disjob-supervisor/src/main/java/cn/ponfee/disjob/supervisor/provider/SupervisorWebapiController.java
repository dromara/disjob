/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.provider;

import cn.ponfee.disjob.common.model.PageResponse;
import cn.ponfee.disjob.common.model.Result;
import cn.ponfee.disjob.common.spring.BaseController;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.openapi.supervisor.SupervisorOpenapi;
import cn.ponfee.disjob.core.openapi.supervisor.request.AddSchedJobRequest;
import cn.ponfee.disjob.core.openapi.supervisor.request.SchedInstancePageRequest;
import cn.ponfee.disjob.core.openapi.supervisor.request.SchedJobPageRequest;
import cn.ponfee.disjob.core.openapi.supervisor.request.UpdateSchedJobRequest;
import cn.ponfee.disjob.core.openapi.supervisor.response.SchedInstanceResponse;
import cn.ponfee.disjob.core.openapi.supervisor.response.SchedJobResponse;
import cn.ponfee.disjob.core.openapi.supervisor.response.SchedTaskResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Supervisor web api
 *
 * @author Ponfee
 */
@Tag(name = "Supervisor web api")
@RestController
@RequestMapping("supervisor/webapi")
public class SupervisorWebapiController extends BaseController {

    private final SupervisorOpenapi supervisorOpenapi;

    public SupervisorWebapiController(SupervisorOpenapi supervisorOpenapi) {
        this.supervisorOpenapi = supervisorOpenapi;
    }

    // ------------------------------------------------------------------job

    @PostMapping("job/add")
    public Result<Void> addJob(@RequestBody AddSchedJobRequest req) throws JobException {
        supervisorOpenapi.addJob(req);
        return Result.success();
    }

    @PutMapping("job/update")
    public Result<Void> updateJob(@RequestBody UpdateSchedJobRequest req) throws JobException {
        supervisorOpenapi.updateJob(req);
        return Result.success();
    }

    @DeleteMapping("job/delete")
    public Result<Void> deleteJob(@RequestParam("jobId") long jobId) {
        supervisorOpenapi.deleteJob(jobId);
        return Result.success();
    }

    @PostMapping("job/state/change")
    public Result<Boolean> changeJobState(@RequestParam("jobId") long jobId,
                                          @RequestParam("jobState") int jobState) {
        return Result.success(supervisorOpenapi.changeJobState(jobId, jobState));
    }

    @PostMapping("job/trigger")
    public Result<Void> triggerJob(@RequestParam("jobId") long jobId) throws JobException {
        supervisorOpenapi.triggerJob(jobId);
        return Result.success();
    }

    @GetMapping("job/get")
    public Result<SchedJobResponse> getJob(@RequestParam("jobId") long jobId) {
        return Result.success(supervisorOpenapi.getJob(jobId));
    }

    /**
     * Form data
     *
     * @param pageRequest the page request
     * @return page result
     */
    @GetMapping("job/page")
    public Result<PageResponse<SchedJobResponse>> queryJobForPage(SchedJobPageRequest pageRequest) {
        return Result.success(supervisorOpenapi.queryJobForPage(pageRequest));
    }

    // ------------------------------------------------------------------ sched instance

    @PostMapping("instance/pause")
    public Result<Boolean> pauseInstance(@RequestParam("instanceId") long instanceId) {
        return Result.success(supervisorOpenapi.pauseInstance(instanceId));
    }

    @PostMapping("instance/cancel")
    public Result<Boolean> cancelInstance(@RequestParam("instanceId") long instanceId) {
        return Result.success(supervisorOpenapi.cancelInstance(instanceId));
    }

    @PostMapping("instance/resume")
    public Result<Boolean> resumeInstance(@RequestParam("instanceId") long instanceId) {
        return Result.success(supervisorOpenapi.resumeInstance(instanceId));
    }

    @DeleteMapping("instance/delete")
    public Result<Void> deleteInstance(@RequestParam("instanceId") long instanceId) {
        supervisorOpenapi.deleteInstance(instanceId);
        return Result.success();
    }

    @PostMapping("instance/state/change")
    public Result<Void> changeInstanceState(@RequestParam("instanceId") long instanceId,
                                            @RequestParam("targetExecuteState") int targetExecuteState) {
        supervisorOpenapi.changeInstanceState(instanceId, targetExecuteState);
        return Result.success();
    }

    @GetMapping("instance/get")
    public Result<SchedInstanceResponse> getInstance(@RequestParam("instanceId") long instanceId) {
        return Result.success(supervisorOpenapi.getInstance(instanceId));
    }

    @GetMapping("instance/tasks")
    public Result<List<SchedTaskResponse>> getInstanceTasks(@RequestParam("instanceId") long instanceId) {
        return Result.success(supervisorOpenapi.getInstanceTasks(instanceId));
    }

    /**
     * Form data
     *
     * @param pageRequest the page request
     * @return result page
     */
    @GetMapping("instance/page")
    public Result<PageResponse<SchedInstanceResponse>> queryInstanceForPage(SchedInstancePageRequest pageRequest) {
        return Result.success(supervisorOpenapi.queryInstanceForPage(pageRequest));
    }

    @GetMapping("instance/children")
    public Result<List<SchedInstanceResponse>> listInstanceChildren(@RequestParam("pnstanceId") long pnstanceId) {
        return Result.success(supervisorOpenapi.listInstanceChildren(pnstanceId));
    }
}
