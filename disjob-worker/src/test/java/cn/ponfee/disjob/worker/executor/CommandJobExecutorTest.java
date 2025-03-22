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

package cn.ponfee.disjob.worker.executor;

import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.worker.executor.impl.CommandJobExecutor;
import cn.ponfee.disjob.worker.util.ProcessUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;

/**
 * @author Ponfee
 */
public class CommandJobExecutorTest {

    @Test
    public void testProcess() {
        Assertions.assertTrue(ProcessUtils.getCurrentJvmProcessId() > 0);
    }

    @Test
    public void testCommand() throws Exception {
        if (!SystemUtils.IS_OS_UNIX) {
            return;
        }

        ExecutionTask task = new ExecutionTask();
        task.setTaskId(1L);

        CommandJobExecutor.CommandParam commandParam = new CommandJobExecutor.CommandParam();
        commandParam.setCmdarray(new String[]{"/bin/sh", "-c", "echo $(date +%Y/%m/%d)"});
        task.setTaskParam(Jsons.toJson(commandParam));

        CommandJobExecutor commandJobExecutor = new CommandJobExecutor();

        ExecutionResult result = commandJobExecutor.execute(task, Savepoint.NOOP);

        String expect = "{\"code\":0,\"msg\":\"" + Dates.format(new Date(), "yyyy/MM/dd") + "\\n\"}";
        Assertions.assertEquals(expect, Jsons.toJson(result));
    }

}
