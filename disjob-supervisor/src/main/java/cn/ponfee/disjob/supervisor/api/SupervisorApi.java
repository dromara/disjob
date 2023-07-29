/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_ )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  \___    http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.api;

import cn.ponfee.disjob.common.model.Result;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.supervisor.api.request.AddSchedJobRequest;
import cn.ponfee.disjob.supervisor.api.request.UpdateSchedJobRequest;
import cn.ponfee.disjob.supervisor.api.response.SchedInstanceResponse;
import cn.ponfee.disjob.supervisor.api.response.SchedJobResponse;
import cn.ponfee.disjob.supervisor.api.response.SchedTaskResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Supervisor api
 *
 * @author Ponfee
 */
@Tag(name = "Supervisor open api")
@RequestMapping(SupervisorApi.PREFIX_PATH)
public interface SupervisorApi {

    String PREFIX_PATH = "/supervisor/api/";

    // ------------------------------------------------------------------job

    @PostMapping("job/add")
    void addJob(AddSchedJobRequest req) throws JobException;

    @PutMapping("job/update")
    void updateJob(UpdateSchedJobRequest req) throws JobException;

    @DeleteMapping("job/delete")
    void deleteJob(long jobId);

    @PostMapping("job/change_state")
    Boolean changeJobState(long jobId, int jobState);

    @PostMapping("job/trigger")
    void triggerJob(long jobId) throws JobException;

    @GetMapping("job/get")
    SchedJobResponse getJob(long jobId);

    // ------------------------------------------------------------------instance

    @PostMapping("instance/pause")
    Boolean pauseInstance(long instanceId);

    @PostMapping("instance/cancel")
    Boolean cancelInstance(long instanceId);

    @PostMapping("instance/resume")
    Boolean resumeInstance(long instanceId);

    @DeleteMapping("instance/delete")
    void deleteInstance(long instanceId);

    @PostMapping("instance/change_state")
    void changeState(long instanceId, int targetExecuteState);

    @GetMapping("instance/get")
    SchedInstanceResponse getInstance(long instanceId);

    @GetMapping("instance/get")
    List<SchedTaskResponse> getTasks(long instanceId);

}
