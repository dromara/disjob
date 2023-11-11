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
import cn.ponfee.disjob.core.api.supervisor.SupervisorOpenRpcService;
import cn.ponfee.disjob.core.api.supervisor.request.AddSchedJobRequest;
import cn.ponfee.disjob.core.api.supervisor.request.SchedInstancePageRequest;
import cn.ponfee.disjob.core.api.supervisor.request.SchedJobPageRequest;
import cn.ponfee.disjob.core.api.supervisor.request.UpdateSchedJobRequest;
import cn.ponfee.disjob.core.api.supervisor.response.SchedInstanceResponse;
import cn.ponfee.disjob.core.api.supervisor.response.SchedJobResponse;
import cn.ponfee.disjob.core.api.supervisor.response.SchedTaskResponse;
import cn.ponfee.disjob.core.exception.JobException;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Supervisor open api
 *
 * @author Ponfee
 */
@Tag(name = "Supervisor open api")
@RestController
@RequestMapping("supervisor/open/api")
public class SupervisorOpenApiProvider extends BaseController {

    private final SupervisorOpenRpcService supervisorOpenRpcService;

    public SupervisorOpenApiProvider(SupervisorOpenRpcService supervisorOpenRpcService) {
        this.supervisorOpenRpcService = supervisorOpenRpcService;
    }

    // ------------------------------------------------------------------job

    @PostMapping("job/add")
    public Result<Void> addJob(@RequestBody AddSchedJobRequest req) throws JobException {
        supervisorOpenRpcService.addJob(req);
        return Result.success();
    }

    @PutMapping("job/update")
    public Result<Void> updateJob(@RequestBody UpdateSchedJobRequest req) throws JobException {
        supervisorOpenRpcService.updateJob(req);
        return Result.success();
    }

    @DeleteMapping("job/delete")
    public Result<Void> deleteJob(@RequestParam("jobId") long jobId) {
        supervisorOpenRpcService.deleteJob(jobId);
        return Result.success();
    }

    @PostMapping("job/state/change")
    public Result<Boolean> changeJobState(@RequestParam("jobId") long jobId,
                                          @RequestParam("jobState") int jobState) {
        return Result.success(supervisorOpenRpcService.changeJobState(jobId, jobState));
    }

    @PostMapping("job/trigger")
    public Result<Void> triggerJob(@RequestParam("jobId") long jobId) throws JobException {
        supervisorOpenRpcService.triggerJob(jobId);
        return Result.success();
    }

    @GetMapping("job/get")
    public Result<SchedJobResponse> getJob(@RequestParam("jobId") long jobId) {
        return Result.success(supervisorOpenRpcService.getJob(jobId));
    }

    /**
     * Http request Content-Type: Http form-data or application/x-www-form-urlencoded
     *
     * @param pageRequest the page request
     * @return page result
     * @see org.springframework.http.MediaType#APPLICATION_FORM_URLENCODED
     * @see org.springframework.http.MediaType#MULTIPART_FORM_DATA
     */
    @GetMapping("job/page")
    public Result<PageResponse<SchedJobResponse>> queryJobForPage(SchedJobPageRequest pageRequest) {
        return Result.success(supervisorOpenRpcService.queryJobForPage(pageRequest));
    }

    // ------------------------------------------------------------------ sched instance

    @PostMapping("instance/pause")
    public Result<Void> pauseInstance(@RequestParam("instanceId") long instanceId) {
        supervisorOpenRpcService.pauseInstance(instanceId);
        return Result.success();
    }

    @PostMapping("instance/cancel")
    public Result<Void> cancelInstance(@RequestParam("instanceId") long instanceId) {
        supervisorOpenRpcService.cancelInstance(instanceId);
        return Result.success();
    }

    @PostMapping("instance/resume")
    public Result<Void> resumeInstance(@RequestParam("instanceId") long instanceId) {
        supervisorOpenRpcService.resumeInstance(instanceId);
        return Result.success();
    }

    @DeleteMapping("instance/delete")
    public Result<Void> deleteInstance(@RequestParam("instanceId") long instanceId) {
        supervisorOpenRpcService.deleteInstance(instanceId);
        return Result.success();
    }

    @PostMapping("instance/state/change")
    public Result<Void> changeInstanceState(@RequestParam("instanceId") long instanceId,
                                            @RequestParam("targetExecuteState") int targetExecuteState) {
        supervisorOpenRpcService.changeInstanceState(instanceId, targetExecuteState);
        return Result.success();
    }

    @GetMapping("instance/get")
    public Result<SchedInstanceResponse> getInstance(@RequestParam(value = "instanceId") long instanceId,
                                                     @RequestParam(value = "withTasks", defaultValue = "false") boolean withTasks) {
        return Result.success(supervisorOpenRpcService.getInstance(instanceId, withTasks));
    }

    @GetMapping("instance/tasks")
    public Result<List<SchedTaskResponse>> getInstanceTasks(@RequestParam("instanceId") long instanceId) {
        return Result.success(supervisorOpenRpcService.getInstanceTasks(instanceId));
    }

    /**
     * Http request Content-Type: Http form-data or application/x-www-form-urlencoded
     *
     * @param pageRequest the page request
     * @return page result
     * @see org.springframework.http.MediaType#APPLICATION_FORM_URLENCODED
     * @see org.springframework.http.MediaType#MULTIPART_FORM_DATA
     */
    @GetMapping("instance/page")
    public Result<PageResponse<SchedInstanceResponse>> queryInstanceForPage(SchedInstancePageRequest pageRequest) {
        return Result.success(supervisorOpenRpcService.queryInstanceForPage(pageRequest));
    }

    @GetMapping("instance/children")
    public Result<List<SchedInstanceResponse>> listInstanceChildren(@RequestParam("pnstanceId") long pnstanceId) {
        return Result.success(supervisorOpenRpcService.listInstanceChildren(pnstanceId));
    }

}
