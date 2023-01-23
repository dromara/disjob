/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.handle.impl;

import cn.ponfee.scheduler.common.base.model.Result;
import cn.ponfee.scheduler.common.util.Files;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.base.JobCodeMsg;
import cn.ponfee.scheduler.core.handle.Checkpoint;
import cn.ponfee.scheduler.core.handle.JobHandler;
import lombok.Data;
import org.apache.commons.io.IOUtils;
import org.springframework.util.Assert;

import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;

/**
 * The job handler for executes system operation command.
 *
 * <p>Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "echo $(date +%Y/%m/%d)"});
 *
 * @author Ponfee
 */
public class CommandJobHandler extends JobHandler<String> {

    @Override
    public Result<String> execute(Checkpoint checkpoint) throws Exception {
        Assert.hasText(task.getTaskParam(), "Command param cannot be empty.");
        CommandParam commandParam = Jsons.fromJson(task.getTaskParam(), CommandParam.class);
        Assert.notEmpty(commandParam.cmdarray, "Command array cannot be empty.");
        Process process = Runtime.getRuntime().exec(commandParam.cmdarray, commandParam.envp);

        Charset charset = Files.charset(commandParam.charset);

        try (InputStream input = process.getInputStream()) {
            String verbose = IOUtils.toString(input, charset);
            int code = process.waitFor();
            if (code == 0) {
                log.info("Command execute success: {} | {}", task.getTaskId(), verbose);
                return Result.success(verbose);
            } else {
                log.error("Command execute failed: {} | {} | {}", task, code, verbose);
                return Result.failure(JobCodeMsg.JOB_EXECUTE_FAILED.getCode(), code + ": " + verbose);
            }
        }
    }

    @Data
    public static class CommandParam implements Serializable {
        private static final long serialVersionUID = 2079640617453920047L;

        private String[] cmdarray;
        private String[] envp;
        private String charset;
    }

}
