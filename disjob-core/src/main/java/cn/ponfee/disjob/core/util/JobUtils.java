/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.util;

import cn.ponfee.disjob.common.concurrent.Threads;
import cn.ponfee.disjob.common.exception.Throwables;
import cn.ponfee.disjob.common.util.NetUtils;
import cn.ponfee.disjob.common.util.ProcessUtils;
import cn.ponfee.disjob.core.base.JobCodeMsg;
import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.handle.ExecuteResult;
import cn.ponfee.disjob.core.handle.execution.ExecutingTask;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Job utility
 *
 * @author Ponfee
 */
public class JobUtils {

    private static final Logger LOG = LoggerFactory.getLogger(JobUtils.class);

    public static ExecuteResult completeProcess(Process process, Charset charset, ExecutingTask executingTask, Logger log) {
        try (InputStream is = process.getInputStream(); InputStream es = process.getErrorStream()) {
            // 一次性获取全部执行结果信息：不是在控制台实时展示执行信息，所以此处不用通过异步线程去获取命令的实时执行信息
            String verbose = IOUtils.toString(is, charset);
            String error = IOUtils.toString(es, charset);
            int code = process.waitFor();
            if (code == ProcessUtils.SUCCESS_CODE) {
                return ExecuteResult.success(verbose);
            } else {
                return ExecuteResult.failure(JobCodeMsg.JOB_EXECUTE_FAILED.getCode(), code + ": " + error);
            }
        } catch (Throwable t) {
            log.error("Process execute error: " + executingTask, t);
            Threads.interruptIfNecessary(t);
            return ExecuteResult.failure(JobCodeMsg.JOB_EXECUTE_ERROR.getCode(), Throwables.getRootCauseMessage(t));
        } finally {
            ProcessUtils.destroy(process);
        }
    }

    public static String getLocalHost(String specifiedHost) {
        String host = specifiedHost;
        if (isValidHost(host, "specified")) {
            return host;
        }

        host = System.getProperty(JobConstants.DISJOB_BOUND_SERVER_HOST);
        if (isValidHost(host, "jvm")) {
            return host;
        }

        host = System.getenv(JobConstants.DISJOB_BOUND_SERVER_HOST);
        if (isValidHost(host, "os")) {
            return host;
        }

        host = NetUtils.getLocalHost();
        if (isValidHost(host, "network")) {
            return host;
        }

        throw new Error("Not found available server host.");
    }

    private static boolean isValidHost(String host, String from) {
        if (StringUtils.isBlank(host)) {
            return false;
        }
        if (NetUtils.isValidLocalHost(host)) {
            return true;
        }
        LOG.warn("Invalid bound server host configured {}: {}", from, host);
        return false;
    }

}
