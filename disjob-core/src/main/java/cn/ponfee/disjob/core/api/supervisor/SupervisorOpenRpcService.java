/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_ )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  \___    http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.api.supervisor;

import cn.ponfee.disjob.common.model.PageResponse;
import cn.ponfee.disjob.core.api.supervisor.request.AddSchedJobRequest;
import cn.ponfee.disjob.core.api.supervisor.request.SchedInstancePageRequest;
import cn.ponfee.disjob.core.api.supervisor.request.SchedJobPageRequest;
import cn.ponfee.disjob.core.api.supervisor.request.UpdateSchedJobRequest;
import cn.ponfee.disjob.core.api.supervisor.response.SchedInstanceResponse;
import cn.ponfee.disjob.core.api.supervisor.response.SchedJobResponse;
import cn.ponfee.disjob.core.api.supervisor.response.SchedTaskResponse;
import cn.ponfee.disjob.core.exception.JobException;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Supervisor open rpc service
 *
 * @author Ponfee
 */
@Hidden
@RequestMapping("supervisor/open/rpc/")
public interface SupervisorOpenRpcService {

    // ------------------------------------------------------------------job

    @PostMapping("job/add")
    void addJob(AddSchedJobRequest req) throws JobException;

    @PutMapping("job/update")
    void updateJob(UpdateSchedJobRequest req) throws JobException;

    @DeleteMapping("job/delete")
    void deleteJob(long jobId);

    @PostMapping("job/state/change")
    Boolean changeJobState(long jobId, int jobState);

    @PostMapping("job/trigger")
    void triggerJob(long jobId) throws JobException;

    @GetMapping("job/get")
    SchedJobResponse getJob(long jobId);

    @GetMapping("job/page")
    PageResponse<SchedJobResponse> queryJobForPage(SchedJobPageRequest pageRequest);

    // ------------------------------------------------------------------instance

    @PostMapping("instance/pause")
    void pauseInstance(long instanceId);

    @PostMapping("instance/cancel")
    void cancelInstance(long instanceId);

    @PostMapping("instance/resume")
    void resumeInstance(long instanceId);

    @DeleteMapping("instance/delete")
    void deleteInstance(long instanceId);

    @PostMapping("instance/state/change")
    void changeInstanceState(long instanceId, int targetExecuteState);

    @GetMapping("instance/get")
    SchedInstanceResponse getInstance(long instanceId, boolean withTasks);

    @GetMapping("instance/tasks")
    List<SchedTaskResponse> getInstanceTasks(long instanceId);

    @GetMapping("instance/page")
    PageResponse<SchedInstanceResponse> queryInstanceForPage(SchedInstancePageRequest pageRequest);

    @GetMapping("instance/children")
    List<SchedInstanceResponse> listInstanceChildren(long pnstanceId);

}
