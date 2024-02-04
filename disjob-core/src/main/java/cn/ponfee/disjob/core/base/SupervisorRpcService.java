/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.base;

import cn.ponfee.disjob.core.enums.Operation;
import cn.ponfee.disjob.core.handle.execution.WorkflowPredecessorNode;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.core.param.supervisor.EventParam;
import cn.ponfee.disjob.core.param.supervisor.StartTaskParam;
import cn.ponfee.disjob.core.param.supervisor.TerminateTaskParam;
import cn.ponfee.disjob.core.param.supervisor.UpdateTaskWorkerParam;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Supervisor rpc service, provides for worker communication.
 *
 * @author Ponfee
 */
@Hidden
@RequestMapping(SupervisorRpcService.PREFIX_PATH)
public interface SupervisorRpcService {

    String PREFIX_PATH = "/supervisor/rpc";

    @GetMapping("/task/get")
    SchedTask getTask(long taskId) throws Exception;

    @PostMapping("/task/start")
    boolean startTask(StartTaskParam param) throws Exception;

    @PostMapping("/task/worker/update")
    void updateTaskWorker(List<UpdateTaskWorkerParam> list) throws Exception;

    /**
     * Finds workflow predecessor nodes
     *
     * @param wnstanceId the workflow lead instance id
     * @param instanceId the current node instance id
     * @return list of predecessor nodes
     * @throws Exception if occur error
     */
    @GetMapping("/workflow/predecessor/nodes")
    List<WorkflowPredecessorNode> findWorkflowPredecessorNodes(long wnstanceId, long instanceId) throws Exception;

    @PostMapping("/task/terminate")
    boolean terminateTask(TerminateTaskParam param) throws Exception;

    @PostMapping("/instance/pause")
    boolean pauseInstance(long instanceId) throws Exception;

    @PostMapping("/instance/cancel")
    boolean cancelInstance(long instanceId, Operation operation) throws Exception;

    @GetMapping("/metrics")
    SupervisorMetrics metrics();

    @PostMapping("/publish")
    void publish(EventParam param);

    // ---------------------------------------------------------------------------savepoint

    @PostMapping("/task/savepoint")
    void savepoint(long taskId, String executeSnapshot) throws Exception;

}
