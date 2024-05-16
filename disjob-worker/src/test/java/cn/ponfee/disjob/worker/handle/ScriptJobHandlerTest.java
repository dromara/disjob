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

package cn.ponfee.disjob.worker.handle;

import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.handle.ExecuteResult;
import cn.ponfee.disjob.core.handle.Savepoint;
import cn.ponfee.disjob.core.handle.execution.ExecutingTask;
import cn.ponfee.disjob.worker.handle.impl.ScriptJobHandler;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * @author Ponfee
 */
public class ScriptJobHandlerTest {

    @Test
    public void testShell() throws Exception {
        if (!SystemUtils.IS_OS_UNIX) {
            return;
        }

        ScriptJobHandler.ScriptParam scriptParam = new ScriptJobHandler.ScriptParam();
        scriptParam.setType(ScriptJobHandler.ScriptType.SHELL);
        scriptParam.setScript("#!/bin/sh\necho \"hello, shell!\"\n");

        ExecutingTask executingTask = new ExecutingTask();
        executingTask.setTaskId(1L);
        executingTask.setTaskParam(Jsons.toJson(scriptParam));

        ScriptJobHandler scriptJobHandler = new ScriptJobHandler();

        ExecuteResult execute = scriptJobHandler.execute(executingTask, Savepoint.DISCARD);
        Assertions.assertEquals("{\"code\":0,\"msg\":\"hello, shell!\\n\"}", Jsons.toJson(execute));
    }

    @Disabled
    @Test
    public void testPython() throws Exception {
        ScriptJobHandler.ScriptParam scriptParam = new ScriptJobHandler.ScriptParam();
        scriptParam.setType(ScriptJobHandler.ScriptType.PYTHON);
        scriptParam.setScript("print('hello, python!')\n");

        ExecutingTask executingTask = new ExecutingTask();
        executingTask.setTaskId(1L);
        executingTask.setTaskParam(Jsons.toJson(scriptParam));

        ScriptJobHandler scriptJobHandler = new ScriptJobHandler();

        ExecuteResult execute = scriptJobHandler.execute(executingTask, Savepoint.DISCARD);
        Assertions.assertEquals("{\"code\":0,\"msg\":\"OK\",\"data\":\"hello, python!\\n\"}", Jsons.toJson(execute));
    }

    @Test
    public void testJson() {
        String script = "#!/bin/sh\necho \"hello shell!\"";
        System.out.println("--------------------");
        System.out.println(script);
        System.out.println("--------------------\n");
        script = StringUtils.replaceEach(script, new String[]{"\r", "\n", "\""}, new String[]{"\\r", "\\n", "\\\""});
        Map<String, String> map = ImmutableMap.of(
            "type", "SHELL",
            "script", script
        );
        System.out.println(Jsons.toJson(map));
        Assertions.assertEquals("#!/bin/sh\\necho \\\"hello shell!\\\"", map.get("script"));
    }

}
