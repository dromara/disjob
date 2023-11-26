/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.base;

import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.handle.SplitTask;
import cn.ponfee.disjob.core.param.worker.JobHandlerParam;
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
        when(workerRpcService.split(any())).thenReturn(Collections.singletonList(new SplitTask(taskParam)));

        List<SplitTask> result = workerRpcService.split(new JobHandlerParam("group", null, null, null, null));
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(taskParam, result.get(0).getTaskParam());
    }

}
