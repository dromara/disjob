/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.base;

import cn.ponfee.disjob.core.enums.Operations;
import cn.ponfee.disjob.core.handle.Checkpoint;
import cn.ponfee.disjob.core.handle.execution.WorkflowPredecessorNode;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.core.param.StartTaskParam;
import cn.ponfee.disjob.core.param.TaskWorkerParam;
import cn.ponfee.disjob.core.param.TerminateTaskParam;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Supervisor service api, for the worker communication.
 *
 * @author Ponfee
 */
@Hidden
@RequestMapping("supervisor/service/")
public interface SupervisorService extends Checkpoint {

    @GetMapping("task/get")
    SchedTask getTask(long taskId) throws Exception;

    @PostMapping("task/start")
    boolean startTask(StartTaskParam param) throws Exception;

    @PostMapping("task/worker/update")
    void updateTaskWorker(List<TaskWorkerParam> params) throws Exception;

    /**
     * Gets workflow predecessor nodes
     *
     * @param wnstanceId the workflow lead instance id
     * @param instanceId the instance id
     * @return list of predecessor nodes
     * @throws Exception if occur error
     */
    @GetMapping("workflow/predecessor/nodes/get")
    List<WorkflowPredecessorNode> getWorkflowPredecessorNodes(long wnstanceId, long instanceId) throws Exception;

    @PostMapping("task/terminate")
    boolean terminateTask(TerminateTaskParam param) throws Exception;

    @PostMapping("instance/pause")
    boolean pauseInstance(long instanceId, Long wnstanceId) throws Exception;

    @PostMapping("instance/cancel")
    boolean cancelInstance(long instanceId, Long wnstanceId, Operations ops) throws Exception;

    // ---------------------------------------------------------------------------checkpoint

    @Override
    @PostMapping("task/checkpoint")
    boolean checkpoint(long taskId, String executeSnapshot) throws Exception;

}
