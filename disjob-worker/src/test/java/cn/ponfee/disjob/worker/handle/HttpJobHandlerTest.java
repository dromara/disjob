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

package cn.ponfee.disjob.worker.handle;

import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.worker.handle.impl.HttpJobHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Ponfee
 */
@Disabled
public class HttpJobHandlerTest {

    @Test
    public void testHttpJobHandler() {
        ExecuteTask task = new ExecuteTask();
        task.setTaskId(1L);
        HttpJobHandler.HttpJobRequest req = new HttpJobHandler.HttpJobRequest();
        req.setMethod("GET");
        req.setUrl("https://www.baidu.com");
        task.setTaskParam(Jsons.toJson(req));
        HttpJobHandler httpJobHandler = new HttpJobHandler();

        ExecuteResult result = httpJobHandler.execute(task, Savepoint.NOOP);
        System.out.println(Jsons.toJson(result));
        Assertions.assertTrue(result.isSuccess());
    }

}
