/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.handle;

import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.handle.execution.ExecutingTask;
import cn.ponfee.disjob.core.handle.impl.CommandJobHandler;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;

/**
 * @author Ponfee
 */
public class CommandJobHandlerTest {

    @Test
    public void testCommand() throws Exception {
        if (!SystemUtils.IS_OS_UNIX) {
            return;
        }

        ExecutingTask executingTask = new ExecutingTask();
        executingTask.setTaskId(1L);

        CommandJobHandler.CommandParam commandParam = new CommandJobHandler.CommandParam();
        commandParam.setCmdarray(new String[]{"/bin/sh", "-c", "echo $(date +%Y/%m/%d)"});
        executingTask.setTaskParam(Jsons.toJson(commandParam));

        CommandJobHandler commandJobHandler = new CommandJobHandler();

        ExecuteResult result = commandJobHandler.execute(executingTask, Savepoint.DISCARD);

        String expect = "{\"code\":0,\"msg\":\"" + Dates.format(new Date(), "yyyy/MM/dd") + "\\n\"}";
        Assertions.assertEquals(expect, Jsons.toJson(result));
    }

}
