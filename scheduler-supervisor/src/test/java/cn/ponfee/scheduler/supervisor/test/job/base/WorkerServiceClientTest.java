/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.test.job.base;

import cn.ponfee.scheduler.core.exception.JobException;
import cn.ponfee.scheduler.core.handle.SplitTask;
import cn.ponfee.scheduler.supervisor.SpringBootTestBase;
import cn.ponfee.scheduler.supervisor.base.WorkerServiceClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;

/**
 * <pre>
 * WorkerServiceClient Test
 *
 * Mock一般用在不依赖框架的单元测试
 * MockBean用在依赖Spring上下文环境，使用@MockBean替换Spring上下文中的Bean（这样会导致Spring上下文重启）
 * </pre>
 *
 * @author Ponfee
 */
public class WorkerServiceClientTest extends SpringBootTestBase<Object> {

    @Test
    public void testSplit() throws JobException {
        String taskParam = "taskParam";
        doReturn(Collections.singletonList(new SplitTask(taskParam))).when(workerService).split(any(), any());

        WorkerServiceClient client = new WorkerServiceClient(workerService, null);
        List<SplitTask> result = client.split("group", null, null);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(taskParam, result.get(0).getTaskParam());
    }

}
