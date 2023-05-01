/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.base;

import cn.ponfee.scheduler.core.enums.Operations;
import cn.ponfee.scheduler.core.handle.Checkpoint;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.core.param.StartTaskParam;
import cn.ponfee.scheduler.core.param.TaskWorkerParam;
import cn.ponfee.scheduler.core.param.TerminateTaskParam;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

/**
 * Supervisor provides api, for the worker communication.
 *
 * @author Ponfee
 */
@Hidden
public interface SupervisorService extends Checkpoint {

    String PREFIX_PATH = "supervisor/rpc/";

    @GetMapping(PREFIX_PATH + "task/get")
    SchedTask getTask(long taskId) throws Exception;

    @PostMapping(PREFIX_PATH + "task/start")
    boolean startTask(StartTaskParam param) throws Exception;

    @PostMapping(PREFIX_PATH + "task_worker/update")
    void updateTaskWorker(List<TaskWorkerParam> params);

    @PostMapping(PREFIX_PATH + "task/terminate")
    boolean terminateTask(TerminateTaskParam param) throws Exception;

    @PostMapping(PREFIX_PATH + "instance/pause")
    boolean pauseInstance(long instanceId, Long wnstanceId) throws Exception;

    @PostMapping(PREFIX_PATH + "instance/cancel")
    boolean cancelInstance(long instanceId, Long wnstanceId, Operations ops) throws Exception;

    // ---------------------------------------------------------------------------checkpoint

    @Override
    @PostMapping(PREFIX_PATH + "task/checkpoint")
    boolean checkpoint(long taskId, String executeSnapshot) throws Exception;

}
