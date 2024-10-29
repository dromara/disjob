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

package cn.ponfee.disjob.supervisor.provider;

import cn.ponfee.disjob.common.model.PageResponse;
import cn.ponfee.disjob.common.model.Result;
import cn.ponfee.disjob.common.spring.BaseController;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.supervisor.application.AuthorizeGroupService;
import cn.ponfee.disjob.supervisor.application.OpenapiService;
import cn.ponfee.disjob.supervisor.application.request.SchedInstancePageRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedJobAddRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedJobPageRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedJobUpdateRequest;
import cn.ponfee.disjob.supervisor.application.response.SchedInstanceResponse;
import cn.ponfee.disjob.supervisor.application.response.SchedJobResponse;
import cn.ponfee.disjob.supervisor.application.response.SchedTaskResponse;
import cn.ponfee.disjob.supervisor.auth.SupervisorAuthentication;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.ponfee.disjob.supervisor.auth.AuthenticationConfigurer.requestGroup;
import static cn.ponfee.disjob.supervisor.auth.AuthenticationConfigurer.requestUser;

/**
 * Supervisor openapi provider.
 *
 * @author Ponfee
 */
@RestController
@RequestMapping("/supervisor/openapi")
@SupervisorAuthentication(SupervisorAuthentication.Subject.USER)
@RequiredArgsConstructor
public class SupervisorOpenapiProvider extends BaseController {

    private final OpenapiService openapiService;
    private final AuthorizeGroupService authorizeGroupService;

    // ------------------------------------------------------------------job

    @PostMapping("/job/add")
    public Result<Long> addJob(@RequestBody SchedJobAddRequest req) throws JobException {
        AuthorizeGroupService.authorizeGroup(requestUser(), requestGroup(), req.getGroup());

        return Result.success(openapiService.addJob(req));
    }

    @PutMapping("/job/update")
    public Result<Void> updateJob(@RequestBody SchedJobUpdateRequest req) throws JobException {
        AuthorizeGroupService.authorizeGroup(requestUser(), requestGroup(), req.getGroup());

        openapiService.updateJob(req);
        return Result.success();
    }

    @DeleteMapping("/job/delete")
    public Result<Void> deleteJob(@RequestParam("jobId") long jobId) {
        authorizeGroupService.authorizeJob(requestUser(), requestGroup(), jobId);

        openapiService.deleteJob(jobId);
        return Result.success();
    }

    @PostMapping("/job/state/change")
    public Result<Boolean> changeJobState(@RequestParam("jobId") long jobId,
                                          @RequestParam("jobState") int jobState) {
        authorizeGroupService.authorizeJob(requestUser(), requestGroup(), jobId);

        return Result.success(openapiService.changeJobState(jobId, jobState));
    }

    @PostMapping("/job/trigger")
    public Result<Void> manualTriggerJob(@RequestParam("jobId") long jobId) throws JobException {
        authorizeGroupService.authorizeJob(requestUser(), requestGroup(), jobId);

        openapiService.manualTriggerJob(jobId);
        return Result.success();
    }

    @GetMapping("/job/get")
    public Result<SchedJobResponse> getJob(@RequestParam("jobId") long jobId) {
        authorizeGroupService.authorizeJob(requestUser(), requestGroup(), jobId);

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
    @GetMapping("/job/page")
    public Result<PageResponse<SchedJobResponse>> queryJobForPage(SchedJobPageRequest pageRequest) {
        pageRequest.authorizeAndTruncateGroup(requestUser());

        return Result.success(openapiService.queryJobForPage(pageRequest));
    }

    // ------------------------------------------------------------------ sched instance

    @PostMapping("/instance/pause")
    public Result<Void> pauseInstance(@RequestParam("instanceId") long instanceId) {
        authorizeGroupService.authorizeInstance(requestUser(), requestGroup(), instanceId);

        openapiService.pauseInstance(instanceId);
        return Result.success();
    }

    @PostMapping("/instance/cancel")
    public Result<Void> cancelInstance(@RequestParam("instanceId") long instanceId) {
        authorizeGroupService.authorizeInstance(requestUser(), requestGroup(), instanceId);

        openapiService.cancelInstance(instanceId);
        return Result.success();
    }

    @PostMapping("/instance/resume")
    public Result<Void> resumeInstance(@RequestParam("instanceId") long instanceId) {
        authorizeGroupService.authorizeInstance(requestUser(), requestGroup(), instanceId);

        openapiService.resumeInstance(instanceId);
        return Result.success();
    }

    @DeleteMapping("/instance/delete")
    public Result<Void> deleteInstance(@RequestParam("instanceId") long instanceId) {
        authorizeGroupService.authorizeInstance(requestUser(), requestGroup(), instanceId);

        openapiService.deleteInstance(instanceId);
        return Result.success();
    }

    @PostMapping("/instance/state/change")
    public Result<Void> changeInstanceState(@RequestParam("instanceId") long instanceId,
                                            @RequestParam("targetExecuteState") int targetExecuteState) {
        authorizeGroupService.authorizeInstance(requestUser(), requestGroup(), instanceId);

        openapiService.changeInstanceState(instanceId, targetExecuteState);
        return Result.success();
    }

    @GetMapping("/instance/get")
    public Result<SchedInstanceResponse> getInstance(@RequestParam(value = "instanceId") long instanceId,
                                                     @RequestParam(value = "includeTasks", defaultValue = "false") boolean includeTasks) {
        authorizeGroupService.authorizeInstance(requestUser(), requestGroup(), instanceId);

        return Result.success(openapiService.getInstance(instanceId, includeTasks));
    }

    @GetMapping("/instance/tasks")
    public Result<List<SchedTaskResponse>> getInstanceTasks(@RequestParam("instanceId") long instanceId) {
        authorizeGroupService.authorizeInstance(requestUser(), requestGroup(), instanceId);

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
    @GetMapping("/instance/page")
    public Result<PageResponse<SchedInstanceResponse>> queryInstanceForPage(SchedInstancePageRequest pageRequest) {
        pageRequest.authorize(requestUser(), authorizeGroupService);

        return Result.success(openapiService.queryInstanceForPage(pageRequest));
    }

    @GetMapping("/instance/children")
    public Result<List<SchedInstanceResponse>> listInstanceChildren(@RequestParam("pnstanceId") long pnstanceId) {
        authorizeGroupService.authorizeInstance(requestUser(), requestGroup(), pnstanceId);

        return Result.success(openapiService.listInstanceChildren(pnstanceId));
    }

}
