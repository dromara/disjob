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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

/**
 * Supervisor provides api, for the worker communication.
 *
 * @author Ponfee
 */
public interface SupervisorService extends Checkpoint {

    String PREFIX_PATH = "supervisor/rpc/";

    @GetMapping(PREFIX_PATH + "job/get")
    SchedJob getJob(long jobId) throws Exception;

    @GetMapping(PREFIX_PATH + "task/get")
    SchedTask getTask(long taskId) throws Exception;

    @PostMapping(PREFIX_PATH + "task/start")
    boolean startTask(ExecuteParam param) throws Exception;

    @PostMapping(PREFIX_PATH + "task_worker/update")
    boolean updateTaskWorker(List<Long> taskIds, String worker);

    @PostMapping(PREFIX_PATH + "task_error_msg/update")
    boolean updateTaskErrorMsg(long taskId, String errorMsg) throws Exception;

    @PostMapping(PREFIX_PATH + "track/pause")
    boolean pauseTrack(long trackId) throws Exception;

    @PostMapping(PREFIX_PATH + "track/cancel")
    boolean cancelTrack(long trackId, Operations operation) throws Exception;

    @PostMapping(PREFIX_PATH + "executing_task/pause")
    boolean pauseExecutingTask(ExecuteParam param, String errorMsg) throws Exception;

    @PostMapping(PREFIX_PATH + "executing_task/cancel")
    boolean cancelExecutingTask(ExecuteParam param, ExecuteState toState, String errorMsg) throws Exception;

    @PostMapping(PREFIX_PATH + "executing_task/terminate")
    boolean terminateExecutingTask(ExecuteParam param, ExecuteState toState, String errorMsg) throws Exception;

    // ---------------------------------------------------------------------------checkpoint

    @Override
    @PostMapping(PREFIX_PATH + "task/checkpoint")
    boolean checkpoint(long taskId, String executeSnapshot) throws Exception;

}
