/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.handle;

import cn.ponfee.disjob.common.model.Result;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.handle.execution.ExecutingTask;
import cn.ponfee.disjob.core.handle.impl.HttpJobHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Ponfee
 */
public class HttpJobHandlerTest {

    @Test
    public void testHttpJobHandler() {
        ExecutingTask executingTask = new ExecutingTask();
        executingTask.setTaskId(1L);
        HttpJobHandler.HttpJobRequest req = new HttpJobHandler.HttpJobRequest();
        req.setMethod("GET");
        req.setUrl("https://www.baidu.com");
        executingTask.setTaskParam(Jsons.toJson(req));
        HttpJobHandler httpJobHandler = new HttpJobHandler();

        Result<String> result = httpJobHandler.execute(executingTask, Savepoint.DISCARD);
        System.out.println(Jsons.toJson(result));
        Assertions.assertTrue(result.isSuccess());
    }

}
