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

import cn.ponfee.disjob.core.dto.worker.*;
import cn.ponfee.disjob.core.exception.JobException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Worker rpc service, provides for supervisor communication.
 *
 * @author Ponfee
 */
@RequestMapping(WorkerRpcService.PREFIX_PATH)
public interface WorkerRpcService {

    String PREFIX_PATH = "/worker/rpc";

    @PostMapping("/supervisor/event/subscribe")
    void subscribeSupervisorEvent(SupervisorEventParam param);

    @PostMapping("/job/verify")
    void verifyJob(VerifyJobParam param) throws JobException;

    @PostMapping("/job/split")
    SplitJobResult splitJob(SplitJobParam param) throws JobException;

    @GetMapping("/task/exists")
    boolean existsTask(ExistsTaskParam param);

    @GetMapping("/metrics/get")
    WorkerMetrics getMetrics(GetMetricsParam param);

    @PostMapping("/worker/configure")
    void configureWorker(ConfigureWorkerParam param);

}
