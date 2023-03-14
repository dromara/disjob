/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.base;

import cn.ponfee.scheduler.core.enums.ExecuteState;
import cn.ponfee.scheduler.core.enums.Operations;
import cn.ponfee.scheduler.core.handle.Checkpoint;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.core.param.TaskWorker;
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

    @GetMapping(PREFIX_PATH + "job/get")
    SchedJob getJob(long jobId) throws Exception;

    @GetMapping(PREFIX_PATH + "task/get")
    SchedTask getTask(long taskId) throws Exception;

    @PostMapping(PREFIX_PATH + "task/start")
    boolean startTask(ExecuteParam param) throws Exception;

    @PostMapping(PREFIX_PATH + "task_worker/update")
    void updateTaskWorker(List<TaskWorker> list);

    @PostMapping(PREFIX_PATH + "task/terminate")
    boolean terminateTask(ExecuteParam param, Operations ops, ExecuteState toState, String errorMsg) throws Exception;

    @PostMapping(PREFIX_PATH + "instance/pause")
    boolean pauseInstance(long instanceId) throws Exception;

    @PostMapping(PREFIX_PATH + "instance/cancel")
    boolean cancelInstance(long instanceId, Operations operation) throws Exception;

    // ---------------------------------------------------------------------------checkpoint

    @Override
    @PostMapping(PREFIX_PATH + "task/checkpoint")
    boolean checkpoint(long taskId, String executeSnapshot) throws Exception;

}
