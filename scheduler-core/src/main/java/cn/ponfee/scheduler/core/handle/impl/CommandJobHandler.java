package cn.ponfee.scheduler.core.handle.impl;

import cn.ponfee.scheduler.common.base.model.Result;
import cn.ponfee.scheduler.core.base.JobCodeMsg;
import cn.ponfee.scheduler.core.handle.Checkpoint;
import cn.ponfee.scheduler.core.handle.JobHandler;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * The job handler for executes system operation command.
 *
 * @author Ponfee
 */
public class CommandJobHandler extends JobHandler<String> {

    private static final Logger LOG = LoggerFactory.getLogger(CommandJobHandler.class);

    @Override
    public Result<String> execute(Checkpoint checkpoint) throws Exception {
        Process process = Runtime.getRuntime().exec(task.getTaskParam());

        try (InputStream input = process.getInputStream()) {
            String verbose = IOUtils.toString(input, StandardCharsets.UTF_8);

            process.waitFor();
            int code = process.exitValue();
            if (code == 0) {
                LOG.info("Command execute success, verbose: {}.", verbose);
                return Result.success(verbose);
            }

            LOG.info("Command execute fail, code: {}, verbose: {}, task: {}.", code, verbose, task);
            return Result.failure(
                JobCodeMsg.JOB_EXECUTE_FAILED.getCode(),
                "Command fail, code: " + code + ", verbose: " + verbose + ", task-id: " + task.getTaskId()
            );
        }
    }

}
