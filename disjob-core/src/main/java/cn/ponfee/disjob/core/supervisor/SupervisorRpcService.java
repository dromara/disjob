/*
 * Copyright 2022-2026 Ponfee (http://www.ponfee.cn/)
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

package cn.ponfee.disjob.core.supervisor;

import cn.ponfee.disjob.core.enums.Operation;
import cn.ponfee.disjob.core.enums.RegistryEventType;
import cn.ponfee.disjob.core.supervisor.dto.StartTaskParam;
import cn.ponfee.disjob.core.supervisor.dto.StartTaskResult;
import cn.ponfee.disjob.core.supervisor.dto.StopTaskParam;
import cn.ponfee.disjob.core.worker.Worker;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Supervisor rpc service, provides for worker remote call supervisor
 *
 * @author Ponfee
 */
@RequestMapping("/supervisor/rpc")
public interface SupervisorRpcService {

    @PostMapping("/worker_event/subscribe")
    void subscribeWorkerEvent(RegistryEventType eventType, Worker worker);

    @PostMapping("/task_worker/update")
    void updateTaskWorker(List<Long> taskIds, String worker);

    @PostMapping("/task/start")
    StartTaskResult startTask(StartTaskParam param);

    @PostMapping("/task/stop")
    boolean stopTask(StopTaskParam param);

    @PostMapping("/instance/pause")
    boolean pauseInstance(long instanceId);

    @PostMapping("/instance/cancel")
    boolean cancelInstance(long instanceId, Operation operation);

    /**
     * Savepoint the task execution snapshot data
     *
     * @param taskId          the taskId
     * @param worker          the worker
     * @param executeSnapshot the execution snapshot data
     * @return {@code true} if saved successful
     */
    @PostMapping("/task/savepoint")
    boolean savepoint(long taskId, String worker, String executeSnapshot);

}
