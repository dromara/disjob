/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.handle.impl;

import cn.ponfee.disjob.common.util.Files;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.common.util.ProcessUtils;
import cn.ponfee.disjob.core.handle.ExecuteResult;
import cn.ponfee.disjob.core.handle.JobHandler;
import cn.ponfee.disjob.core.handle.Savepoint;
import cn.ponfee.disjob.core.handle.execution.ExecutingTask;
import cn.ponfee.disjob.core.util.JobUtils;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.nio.charset.Charset;

/**
 * <pre>
 *  The job handler for executes system operation command.
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
public class CommandJobHandler extends JobHandler {
    private final static Logger LOG = LoggerFactory.getLogger(CommandJobHandler.class);

    private Charset charset;
    private Long pid;

    @Override
    protected void onStop() {
        if (pid != null) {
            ProcessUtils.killProcess(pid, charset);
        }
    }

    @Override
    public ExecuteResult execute(ExecutingTask executingTask, Savepoint savepoint) throws Exception {
        String taskParam = executingTask.getTaskParam();
        Assert.hasText(taskParam, "Command param cannot be empty.");
        CommandParam commandParam = Jsons.JSON5.readValue(taskParam, CommandParam.class);
        Assert.notEmpty(commandParam.cmdarray, "Command array cannot be empty.");
        this.charset = Files.charset(commandParam.charset);

        Process process = Runtime.getRuntime().exec(commandParam.cmdarray, commandParam.envp);
        this.pid = ProcessUtils.getProcessId(process);
        LOG.info("Command process id: {} | {}", executingTask.getTaskId(), pid);
        return JobUtils.completeProcess(process, charset, executingTask, LOG);
    }

    @Data
    public static class CommandParam implements Serializable {
        private static final long serialVersionUID = 2079640617453920047L;

        private String[] cmdarray;
        private String[] envp;
        private String charset;
    }

}
