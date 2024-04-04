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
@RequestMapping("/supervisor/rpc")
public interface SupervisorRpcService {

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

    /**
     * Savepoint the task execution snapshot data
     *
     * @param taskId          the taskId
     * @param executeSnapshot the execution snapshot data
     * @throws Exception if occur exception
     */
    @PostMapping("/task/savepoint")
    void savepoint(long taskId, String executeSnapshot) throws Exception;

}
