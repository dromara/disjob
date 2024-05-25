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

package cn.ponfee.disjob.dispatch.http;

import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.common.spring.RpcController;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.base.WorkerRpcService;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import cn.ponfee.disjob.dispatch.TaskReceiver;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Task receiver based http.
 *
 * @author Ponfee
 */
@RpcController
public class HttpTaskReceiver extends TaskReceiver implements Controller {

    public HttpTaskReceiver(Worker.Current currentWorker, TimingWheel<ExecuteTaskParam> timingWheel) {
        super(currentWorker, timingWheel);
    }

    @Override
    public boolean receive(ExecuteTaskParam param) {
        return super.doReceive(param);
    }

}

@Hidden
@RequestMapping(WorkerRpcService.PREFIX_PATH)
interface Controller {

    /**
     * Receive task http method
     *
     * @param param the task
     * @return {@code true} if received successfully
     */
    @PostMapping("/task/receive")
    boolean receive(ExecuteTaskParam param);

}
