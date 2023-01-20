/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.handle.impl;

import cn.ponfee.scheduler.common.base.model.Result;
import cn.ponfee.scheduler.core.base.JobCodeMsg;
import cn.ponfee.scheduler.core.handle.Checkpoint;
import cn.ponfee.scheduler.core.handle.JobHandler;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * The job handler for executes system operation command.
 *
 * @author Ponfee
 */
public class CommandJobHandler extends JobHandler<String> {

    @Override
    public Result<String> execute(Checkpoint checkpoint) throws Exception {
        Process process = Runtime.getRuntime().exec(task.getTaskParam());
        try (InputStream input = process.getInputStream()) {
            String verbose = IOUtils.toString(input, StandardCharsets.UTF_8);
            process.waitFor();
            int code = process.exitValue();
            if (code == 0) {
                log.info("Command execute success: {} | {}", task.getId(), verbose);
                return Result.success(verbose);
            } else {
                log.error("Command execute failed: {} | {} | {}", task, code, verbose);
                return Result.failure(JobCodeMsg.JOB_EXECUTE_FAILED.getCode(), code + ": " + verbose);
            }
        }
    }

}
