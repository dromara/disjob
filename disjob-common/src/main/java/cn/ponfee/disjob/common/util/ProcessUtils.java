/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

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
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Process execute utility.
 *
 * @author Ponfee
 */
public final class ProcessUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessUtils.class);
    private static final long INVALID_PID = -1L;
    public static final int SUCCESS_CODE = 0;

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
                Process killProcess = new ProcessBuilder("taskkill", "/PID", String.valueOf(pid), "/F", "/T").start();
                waitFor(killProcess, () -> "kill process id " + pid);
                LOG.info("Stop windows process verbose: {} | {}", pid, processVerbose(killProcess, charset));
                destroy(killProcess);
            } else if (SystemUtils.IS_OS_UNIX) {
                // 1、find child process id
                String findChildPidCommand = String.format("ps axo pid,ppid | awk '{ if($2==%d) print$1}'", pid);
                Process findChildPidProcess = new ProcessBuilder("/bin/sh", "-c", findChildPidCommand).start();
                waitFor(findChildPidProcess, () -> "find child process id for " + pid);
                try (InputStream inputStream = findChildPidProcess.getInputStream()) {
                    // stop all child process
                    List<String> childPidList = IOUtils.readLines(inputStream, charset);
                    childPidList.stream().filter(StringUtils::isNotBlank).forEach(e -> killProcess(Long.parseLong(e.trim()), charset));
                }
                destroy(findChildPidProcess);

                // 2、kill current process id
                Process killProcess = new ProcessBuilder("kill", "-9", String.valueOf(pid)).start();
                waitFor(killProcess, () -> "kill process id " + pid);
                LOG.info("Stop unix process verbose: {} | {}", pid, processVerbose(killProcess, charset));
                destroy(killProcess);
            } else {
                LOG.error("Stop process id unknown os name: {}, {}", SystemUtils.OS_NAME, pid);
            }
        } catch (InterruptedException e) {
            LOG.error("Kill process id '" + pid + "' interrupted.");
            ExceptionUtils.rethrow(e);
        } catch (Throwable t) {
            LOG.error("Kill process id '" + pid + "' error.", t);
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

    private static String processVerbose(Process process, Charset charset) throws IOException {
        try (InputStream input = process.getInputStream()) {
            return IOUtils.toString(input, charset);
        }
    }

    private static void waitFor(Process process, Supplier<String> messageSupplier) throws InterruptedException {
        int code = process.waitFor();
        if (code != SUCCESS_CODE) {
            LOG.error("Process execute failed[{}]: {}", code, messageSupplier.get());
        }
    }

}
