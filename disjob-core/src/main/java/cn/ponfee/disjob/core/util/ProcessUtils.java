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
import cn.ponfee.disjob.common.util.Fields;
import cn.ponfee.disjob.common.util.Files;
import cn.ponfee.disjob.core.base.JobCodeMsg;
import cn.ponfee.disjob.core.handle.ExecuteResult;
import cn.ponfee.disjob.core.handle.execution.ExecutingTask;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

/**
 * Process execute utility.
 *
 * @author Ponfee
 */
public final class ProcessUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessUtils.class);
    private static final long INVALID_PID = -1L;

    public static void destroy(Process process) {
        if (process == null) {
            return;
        }
        try {
            process.destroy();
        } catch (Throwable t) {
            LOG.error("Destroy process " + process.getClass().getName() + " error.", t);
        }
    }

    public static ExecuteResult complete(Process process, Charset charset, ExecutingTask executingTask, Logger log) {
        try (InputStream is = process.getInputStream(); InputStream es = process.getErrorStream()) {
            // 一次性获取全部执行结果信息：不是在控制台实时展示执行信息，所以此处不用通过异步线程去获取命令的实时执行信息
            String verbose = IOUtils.toString(is, charset);
            String error = IOUtils.toString(es, charset);
            int code = process.waitFor();
            if (code == 0) {
                return ExecuteResult.success(verbose);
            } else {
                return ExecuteResult.failure(JobCodeMsg.JOB_EXECUTE_FAILED.getCode(), code + ": " + error);
            }
        } catch (Throwable t) {
            log.error("Process execute error: " + executingTask, t);
            Threads.interruptIfNecessary(t);
            return ExecuteResult.failure(JobCodeMsg.JOB_EXECUTE_ERROR.getCode(), Throwables.getRootCauseMessage(t));
        } finally {
            if (process != null) {
                try {
                    process.destroy();
                } catch (Throwable t) {
                    log.error("Destroy process error: " + executingTask, t);
                }
            }
        }
    }

    public static int progress(Process process, Charset charset, Logger log) {
        return progress(process, charset, log::info, log::error);
    }

    public static int progress(Process process, Charset charset, Consumer<String> verbose, Consumer<String> error) {
        // 控制台实时展示
        ForkJoinPool.commonPool().execute(() -> read(process.getInputStream(), charset, verbose));
        ForkJoinPool.commonPool().execute(() -> read(process.getErrorStream(), charset, error));
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            error.accept("Process execute interrupted: " + ExceptionUtils.getStackTrace(e));
            Thread.currentThread().interrupt();
            // return error code: -1
            return -1;
        } finally {
            if (process != null) {
                try {
                    process.destroy();
                } catch (Throwable t) {
                    verbose.accept("Destroy process error: " + ExceptionUtils.getStackTrace(t));
                }
            }
        }
    }

    /**
     * Gets process id
     *
     * @param process the process
     * @return process id
     */
    public static Long getProcessId(Process process) {
        try {
            if (SystemUtils.IS_OS_WINDOWS) {
                // Windows Process class：java.lang.Win32Process || java.lang.ProcessImpl
                long handlePointer = (Long) Fields.get(process, "handle");
                WinNT.HANDLE handle = new WinNT.HANDLE();
                handle.setPointer(Pointer.createConstant(handlePointer));
                return (long) Kernel32.INSTANCE.GetProcessId(handle);
            } else if (SystemUtils.IS_OS_UNIX) {
                // Unix Process class：java.lang.UNIXProcess
                Integer pid = (Integer) Fields.get(process, "pid");
                return pid.longValue();
            } else {
                LOG.error("Get process id unknown os name: {}, {}", SystemUtils.OS_NAME, process.getClass().getName());
                return INVALID_PID;
            }
        } catch (Throwable t) {
            LOG.error("Get process id error.", t);
            return INVALID_PID;
        }
    }

    /**
     * Kill process
     *
     * @param pid     the process id
     * @param charset the charset
     */
    public static void killProcess(Long pid, Charset charset) {
        try {
            if (SystemUtils.IS_OS_WINDOWS) {
                List<String> killCommand = Arrays.asList("taskkill", "/PID", String.valueOf(pid), "/F", "/T");
                Process process = new ProcessBuilder(killCommand).start();
                try (InputStream input = process.getInputStream()) {
                    String verbose = IOUtils.toString(input, charset);
                    LOG.info("Stop windows process verbose: {}", verbose);
                }
                destroy(process);
            } else if (SystemUtils.IS_OS_UNIX) {
                // find child process id
                List<String> killCommand = Arrays.asList("/bin/sh", "-c", String.format("ps axo pid,ppid| awk '{ if($2==%d) print$1}'", pid));
                Process process = new ProcessBuilder(killCommand).start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), charset))) {
                    // stop all child process
                    String childPid;
                    while ((childPid = reader.readLine()) != null) {
                        if (StringUtils.isNotBlank(childPid)) {
                            killProcess(Long.valueOf(childPid.trim()), charset);
                        }
                    }
                }
                // kill current process id
                ProcessBuilder killProcessBuilder = new ProcessBuilder("kill", "-9", String.valueOf(pid));
                killProcessBuilder.start().waitFor();
                LOG.info("Stop unix process id: {}", pid);
                destroy(process);
            } else {
                LOG.error("Stop process id unknown os name: {}, {}", SystemUtils.OS_NAME, pid);
            }
        } catch (Throwable t) {
            LOG.error("Kill process id '" + pid + "' error.", t);
            Threads.interruptIfNecessary(t);
        }
    }

    // -------------------------------------------------------------------------private methods

    private static void read(InputStream input, Charset charset, Consumer<String> consumer) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, charset))) {
            String line;
            while (!Thread.currentThread().isInterrupted() && (line = reader.readLine()) != null) {
                consumer.accept(line);
                consumer.accept(Files.SYSTEM_LINE_SEPARATOR);
            }
        } catch (IOException e) {
            consumer.accept("Read output error: " + ExceptionUtils.getStackTrace(e));
        }
    }

}
