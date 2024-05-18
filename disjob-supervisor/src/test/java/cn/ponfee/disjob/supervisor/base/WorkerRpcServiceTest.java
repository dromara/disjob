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

package cn.ponfee.disjob.supervisor.base;

import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.param.worker.JobHandlerParam;
import cn.ponfee.disjob.core.param.worker.SplitTaskParam;
import cn.ponfee.disjob.supervisor.SpringBootTestBase;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

/**
 * <pre>
 * WorkerRpcService Test
 *
 * Mock一般用在不依赖框架的单元测试
 * MockBean用在依赖Spring上下文环境，使用@MockBean替换Spring上下文中的Bean（这样会导致Spring上下文重启）
 * </pre>
 *
 * @author Ponfee
 */
public class WorkerRpcServiceTest extends SpringBootTestBase<Object> {

    @Test
    public void testSplit() throws JobException {
        String taskParam = "taskParam";
        //doReturn(Collections.singletonList(new SplitTask(taskParam))).when(workerRpcService).split(any());
        when(workerRpcService.split(any())).thenReturn(Collections.singletonList(new SplitTaskParam(taskParam)));

        List<SplitTaskParam> result = workerRpcService.split(new JobHandlerParam("group", null, null, null, null));
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(taskParam, result.get(0).getTaskParam());
    }

}
