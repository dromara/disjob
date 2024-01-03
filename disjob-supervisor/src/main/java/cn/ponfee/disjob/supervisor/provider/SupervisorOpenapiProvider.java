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
import cn.ponfee.disjob.supervisor.application.OpenapiService;
import cn.ponfee.disjob.supervisor.application.request.AddSchedJobRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedInstancePageRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedJobPageRequest;
import cn.ponfee.disjob.supervisor.application.request.UpdateSchedJobRequest;
import cn.ponfee.disjob.supervisor.application.response.SchedInstanceResponse;
import cn.ponfee.disjob.supervisor.application.response.SchedJobResponse;
import cn.ponfee.disjob.supervisor.application.response.SchedTaskResponse;
import cn.ponfee.disjob.supervisor.auth.SupervisorAuthentication;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Supervisor openapi provider.
 *
 * @author Ponfee
 */
@Tag(name = "Supervisor openapi provider")
@RestController
@RequestMapping("supervisor/openapi")
@SupervisorAuthentication(SupervisorAuthentication.Subject.USER)
public class SupervisorOpenapiProvider extends BaseController {

    private final OpenapiService openapiService;

    public SupervisorOpenapiProvider(OpenapiService openapiService) {
        this.openapiService = openapiService;
    }

    // ------------------------------------------------------------------job

    @PostMapping("job/add")
    public Result<Long> addJob(@RequestBody AddSchedJobRequest req) throws JobException {
        return Result.success(openapiService.addJob(req));
    }

    @PutMapping("job/update")
    public Result<Void> updateJob(@RequestBody UpdateSchedJobRequest req) throws JobException {
        openapiService.updateJob(req);
        return Result.success();
    }

    @DeleteMapping("job/delete")
    public Result<Void> deleteJob(@RequestParam("jobId") long jobId) {
        openapiService.deleteJob(jobId);
        return Result.success();
    }

    @PostMapping("job/state/change")
    public Result<Boolean> changeJobState(@RequestParam("jobId") long jobId,
                                          @RequestParam("jobState") int jobState) {
        return Result.success(openapiService.changeJobState(jobId, jobState));
    }

    @PostMapping("job/trigger")
    public Result<Void> triggerJob(@RequestParam("jobId") long jobId) throws JobException {
        openapiService.triggerJob(jobId);
        return Result.success();
    }

    @GetMapping("job/get")
    public Result<SchedJobResponse> getJob(@RequestParam("jobId") long jobId) {
        return Result.success(openapiService.getJob(jobId));
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
        return Result.success(openapiService.queryJobForPage(pageRequest));
    }

    // ------------------------------------------------------------------ sched instance

    @PostMapping("instance/pause")
    public Result<Void> pauseInstance(@RequestParam("instanceId") long instanceId) {
        openapiService.pauseInstance(instanceId);
        return Result.success();
    }

    @PostMapping("instance/cancel")
    public Result<Void> cancelInstance(@RequestParam("instanceId") long instanceId) {
        openapiService.cancelInstance(instanceId);
        return Result.success();
    }

    @PostMapping("instance/resume")
    public Result<Void> resumeInstance(@RequestParam("instanceId") long instanceId) {
        openapiService.resumeInstance(instanceId);
        return Result.success();
    }

    @DeleteMapping("instance/delete")
    public Result<Void> deleteInstance(@RequestParam("instanceId") long instanceId) {
        openapiService.deleteInstance(instanceId);
        return Result.success();
    }

    @PostMapping("instance/state/change")
    public Result<Void> changeInstanceState(@RequestParam("instanceId") long instanceId,
                                            @RequestParam("targetExecuteState") int targetExecuteState) {
        openapiService.changeInstanceState(instanceId, targetExecuteState);
        return Result.success();
    }

    @GetMapping("instance/get")
    public Result<SchedInstanceResponse> getInstance(@RequestParam(value = "instanceId") long instanceId,
                                                     @RequestParam(value = "withTasks", defaultValue = "false") boolean withTasks) {
        return Result.success(openapiService.getInstance(instanceId, withTasks));
    }

    @GetMapping("instance/tasks")
    public Result<List<SchedTaskResponse>> getInstanceTasks(@RequestParam("instanceId") long instanceId) {
        return Result.success(openapiService.getInstanceTasks(instanceId));
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
        return Result.success(openapiService.queryInstanceForPage(pageRequest));
    }

    @GetMapping("instance/children")
    public Result<List<SchedInstanceResponse>> listInstanceChildren(@RequestParam("pnstanceId") long pnstanceId) {
        return Result.success(openapiService.listInstanceChildren(pnstanceId));
    }

}
