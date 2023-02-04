/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.handle;

import cn.ponfee.scheduler.common.base.model.Result;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.handle.impl.HttpJobHandler;
import cn.ponfee.scheduler.core.model.SchedTask;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Ponfee
 */
public class HttpJobHandlerTest {

    @Test
    public void testHttpJobHandler() {
        SchedTask task = new SchedTask();
        task.setTaskId(1L);
        HttpJobHandler.HttpJobRequest req = new HttpJobHandler.HttpJobRequest();
        req.setMethod("GET");
        req.setUrl("https://www.baidu.com");
        task.setTaskParam(Jsons.toJson(req));
        HttpJobHandler httpJobHandler = new HttpJobHandler();
        httpJobHandler.task(task);

        Result<String> result = httpJobHandler.execute(Checkpoint.DISCARD);
        System.out.println(Jsons.toJson(result));
        Assertions.assertTrue(result.isSuccess());
    }

}
