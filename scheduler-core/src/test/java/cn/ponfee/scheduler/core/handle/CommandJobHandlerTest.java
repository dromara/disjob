/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.handle;

import cn.ponfee.scheduler.common.base.model.Result;
import cn.ponfee.scheduler.common.date.Dates;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.handle.impl.CommandJobHandler;
import cn.ponfee.scheduler.core.model.SchedTask;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.Date;

/**
 * @author Ponfee
 */
public class CommandJobHandlerTest {

    @Test
    public void testCommand() throws Exception {
        SchedTask task = new SchedTask();
        task.setTaskId(1L);

        CommandJobHandler.CommandParam commandParam = new CommandJobHandler.CommandParam();
        commandParam.setCmdarray(new String[]{"/bin/sh", "-c", "echo $(date +%Y/%m/%d)"});
        task.setTaskParam(Jsons.toJson(commandParam));

        CommandJobHandler commandJobHandler = new CommandJobHandler();
        commandJobHandler.task(task);

        Result<String> result = commandJobHandler.execute(Checkpoint.DISCARD);
        Assert.assertEquals("{\"code\":0,\"msg\":\"OK\",\"data\":\"2023/01/25\\n\"}", Jsons.toJson(result));
    }

}
