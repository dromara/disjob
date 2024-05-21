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

import cn.ponfee.disjob.core.dto.worker.SplitJobParam;
import cn.ponfee.disjob.core.dto.worker.SplitJobResult;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.supervisor.SpringBootTestBase;
import org.junit.jupiter.api.Test;

import java.util.Collections;

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
        when(workerRpcService.split(any())).thenReturn(new SplitJobResult(Collections.singletonList(taskParam)));

        SplitJobResult result = workerRpcService.split(new SplitJobParam("group", null, null, null, null));
        assertNotNull(result);
        assertNotNull(result.getTaskParams());
        assertEquals(1, result.getTaskParams().size());
        assertEquals(taskParam, result.getTaskParams().get(0));
    }

}
