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
import cn.ponfee.disjob.core.exception.JobCheckedException;
import cn.ponfee.disjob.core.rpc.supervisor.SupervisorRpcApi;
import cn.ponfee.disjob.core.rpc.supervisor.request.AddSchedJobRequest;
import cn.ponfee.disjob.core.rpc.supervisor.request.SchedInstancePageRequest;
import cn.ponfee.disjob.core.rpc.supervisor.request.SchedJobPageRequest;
import cn.ponfee.disjob.core.rpc.supervisor.request.UpdateSchedJobRequest;
import cn.ponfee.disjob.core.rpc.supervisor.response.SchedInstanceResponse;
import cn.ponfee.disjob.core.rpc.supervisor.response.SchedJobResponse;
import cn.ponfee.disjob.core.rpc.supervisor.response.SchedTaskResponse;
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
@RequestMapping("supervisor/web")
public class SupervisorWebApiController extends BaseController {

    private final SupervisorRpcApi supervisorRpcApi;

    public SupervisorWebApiController(SupervisorRpcApi supervisorRpcApi) {
        this.supervisorRpcApi = supervisorRpcApi;
    }

    // ------------------------------------------------------------------job

    @PostMapping("job/add")
    public Result<Void> addJob(@RequestBody AddSchedJobRequest req) throws JobCheckedException {
        supervisorRpcApi.addJob(req);
        return Result.success();
    }

    @PutMapping("job/update")
    public Result<Void> updateJob(@RequestBody UpdateSchedJobRequest req) throws JobCheckedException {
        supervisorRpcApi.updateJob(req);
        return Result.success();
    }

    @DeleteMapping("job/delete")
    public Result<Void> deleteJob(@RequestParam("jobId") long jobId) {
        supervisorRpcApi.deleteJob(jobId);
        return Result.success();
    }

    @PostMapping("job/state/change")
    public Result<Boolean> changeJobState(@RequestParam("jobId") long jobId,
                                          @RequestParam("jobState") int jobState) {
        return Result.success(supervisorRpcApi.changeJobState(jobId, jobState));
    }

    @PostMapping("job/trigger")
    public Result<Void> triggerJob(@RequestParam("jobId") long jobId) throws JobCheckedException {
        supervisorRpcApi.triggerJob(jobId);
        return Result.success();
    }

    @GetMapping("job/get")
    public Result<SchedJobResponse> getJob(@RequestParam("jobId") long jobId) {
        return Result.success(supervisorRpcApi.getJob(jobId));
    }

    /**
     * Form data
     *
     * @param pageRequest the page request
     * @return page result
     */
    @GetMapping("job/page")
    public Result<PageResponse<SchedJobResponse>> queryJobForPage(SchedJobPageRequest pageRequest) {
        return Result.success(supervisorRpcApi.queryJobForPage(pageRequest));
    }

    // ------------------------------------------------------------------ sched instance

    @PostMapping("instance/pause")
    public Result<Void> pauseInstance(@RequestParam("instanceId") long instanceId) {
        supervisorRpcApi.pauseInstance(instanceId);
        return Result.success();
    }

    @PostMapping("instance/cancel")
    public Result<Void> cancelInstance(@RequestParam("instanceId") long instanceId) {
        supervisorRpcApi.cancelInstance(instanceId);
        return Result.success();
    }

    @PostMapping("instance/resume")
    public Result<Void> resumeInstance(@RequestParam("instanceId") long instanceId) {
        supervisorRpcApi.resumeInstance(instanceId);
        return Result.success();
    }

    @DeleteMapping("instance/delete")
    public Result<Void> deleteInstance(@RequestParam("instanceId") long instanceId) {
        supervisorRpcApi.deleteInstance(instanceId);
        return Result.success();
    }

    @PostMapping("instance/state/change")
    public Result<Void> changeInstanceState(@RequestParam("instanceId") long instanceId,
                                            @RequestParam("targetExecuteState") int targetExecuteState) {
        supervisorRpcApi.changeInstanceState(instanceId, targetExecuteState);
        return Result.success();
    }

    @GetMapping("instance/get")
    public Result<SchedInstanceResponse> getInstance(@RequestParam("instanceId") long instanceId) {
        return Result.success(supervisorRpcApi.getInstance(instanceId));
    }

    @GetMapping("instance/tasks")
    public Result<SchedInstanceResponse> getInstanceTasks(@RequestParam("instanceId") long instanceId) {
        return Result.success(supervisorRpcApi.getInstanceTasks(instanceId));
    }

    @GetMapping("tasks/get")
    public Result<List<SchedTaskResponse>> getTasks(@RequestParam("instanceId") long instanceId) {
        return Result.success(supervisorRpcApi.getTasks(instanceId));
    }

    /**
     * Form data
     *
     * @param pageRequest the page request
     * @return result page
     */
    @GetMapping("instance/page")
    public Result<PageResponse<SchedInstanceResponse>> queryInstanceForPage(SchedInstancePageRequest pageRequest) {
        return Result.success(supervisorRpcApi.queryInstanceForPage(pageRequest));
    }

    @GetMapping("instance/children")
    public Result<List<SchedInstanceResponse>> listInstanceChildren(@RequestParam("pnstanceId") long pnstanceId) {
        return Result.success(supervisorRpcApi.listInstanceChildren(pnstanceId));
    }

}
