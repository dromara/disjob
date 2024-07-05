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

import cn.ponfee.disjob.core.dto.supervisor.*;
import cn.ponfee.disjob.core.enums.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Supervisor rpc service, provides for worker communication.
 *
 * @author Ponfee
 */
@RequestMapping("/supervisor/rpc")
public interface SupervisorRpcService {

    @PostMapping("/task/start")
    StartTaskResult startTask(StartTaskParam param) throws Exception;

    @PostMapping("/task/worker/update")
    void updateTaskWorker(List<UpdateTaskWorkerParam> list) throws Exception;

    @PostMapping("/task/stop")
    boolean stopTask(StopTaskParam param) throws Exception;

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
     * @return {@code true} if saved successful
     * @throws Exception if occur exception
     */
    @PostMapping("/task/savepoint")
    boolean savepoint(long taskId, String executeSnapshot) throws Exception;

}
