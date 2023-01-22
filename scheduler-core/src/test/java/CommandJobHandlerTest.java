/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

import cn.ponfee.scheduler.common.base.model.Result;
import cn.ponfee.scheduler.common.date.Dates;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.handle.Checkpoint;
import cn.ponfee.scheduler.core.handle.impl.CommandJobHandler;
import cn.ponfee.scheduler.core.model.SchedTask;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;

/**
 * @author Ponfee
 */
public class CommandJobHandlerTest {

    @Test
    public void testCommand() throws Exception {
        SchedTask task = new SchedTask();
        task.setTaskId(1L);
        task.setTaskParam(Jsons.toJson(Arrays.asList("/bin/sh", "-c", "echo $(date +%Y/%m/%d)")));
        CommandJobHandler commandJobHandler = new CommandJobHandler();
        commandJobHandler.task(task);

        Result<String> result = commandJobHandler.execute(Checkpoint.DISCARD);
        String date = Dates.format(new Date(), "yyyy/MM/dd");
        Assert.assertEquals("{\"code\":0,\"msg\":\"OK\",\"data\":\"" + date + "\\n\"}", Jsons.toJson(result));
    }

}
