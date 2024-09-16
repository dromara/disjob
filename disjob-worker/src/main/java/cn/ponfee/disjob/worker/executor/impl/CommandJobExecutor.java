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

package cn.ponfee.disjob.worker.executor.impl;

import cn.ponfee.disjob.common.util.Files;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.common.util.ProcessUtils;
import cn.ponfee.disjob.worker.executor.*;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.nio.charset.Charset;

/**
 * <pre>
 *  The job executor for executes system operation command.
 *
 *  /bin/bash -c "echo $(date +%Y/%m/%d)"
 *  Runtime.exec(new String[]{"/bin/sh", "-c", "echo $(date +%Y/%m/%d)"});
 *
 *  bin/bash -c 后面接 命令
 *  /bin/bash 后面接 执行的脚本
 * </pre>
 *
 * <pre>job_param example: {@code
 *  {
 *    "cmdarray":[
 *      "/bin/sh",
 *      "-c",
 *      "echo $(date +%Y/%m/%d)"
 *    ]
 *  }
 * }</pre>
 *
 * @author Ponfee
 */
public class CommandJobExecutor extends BasicJobExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(CommandJobExecutor.class);

    private Charset charset;
    private Long pid;

    @Override
    protected void onStop() {
        final Long processId = pid;
        if (processId != null) {
            ProcessUtils.killProcess(processId, charset);
        }
    }

    @Override
    public ExecutionResult execute(ExecutionTask task, Savepoint savepoint) throws Exception {
        String taskParam = task.getTaskParam();
        Assert.hasText(taskParam, "Command param cannot be empty.");
        CommandParam commandParam = Jsons.JSON5.readValue(taskParam, CommandParam.class);
        Assert.notEmpty(commandParam.cmdarray, "Command array cannot be empty.");
        this.charset = Files.charset(commandParam.charset);

        Process process = Runtime.getRuntime().exec(commandParam.cmdarray, commandParam.envp);
        this.pid = ProcessUtils.getProcessId(process);
        LOG.info("Command process id: {}, {}", task.getTaskId(), pid);
        return JobExecutorUtils.completeProcess(process, charset, task, LOG);
    }

    @Getter
    @Setter
    public static class CommandParam implements Serializable {
        private static final long serialVersionUID = 2079640617453920047L;

        private String[] cmdarray;
        private String[] envp;
        private String charset;
    }

}
