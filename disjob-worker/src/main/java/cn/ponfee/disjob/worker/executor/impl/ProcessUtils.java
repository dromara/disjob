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

import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.concurrent.Threads;
import cn.ponfee.disjob.common.exception.Throwables;
import cn.ponfee.disjob.common.util.Files;
import cn.ponfee.disjob.core.base.JobCodeMsg;
import cn.ponfee.disjob.worker.executor.ExecutionResult;
import cn.ponfee.disjob.worker.executor.ExecutionTask;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Process execute utility.
 *
 * @author Ponfee
 */
public final class ProcessUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessUtils.class);
    public static final int SUCCESS_CODE = 0;

    public static int progress(Process process, Charset charset, Logger log) {
        return progress(process, charset, log::info, log::error);
    }

    public static int progress(Process process, Charset charset, Consumer<String> verbose, Consumer<String> error) {
        // 控制台实时展示
        ThreadPoolExecutors.commonThreadPool().execute(() -> read(process.getInputStream(), charset, verbose));
        ThreadPoolExecutors.commonThreadPool().execute(() -> read(process.getErrorStream(), charset, error));
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            error.accept("Process execute interrupted: " + ExceptionUtils.getStackTrace(e));
            Thread.currentThread().interrupt();
            // return error code: -1
            return -1;
        } finally {
            try {
                process.destroy();
            } catch (Throwable t) {
                verbose.accept("Destroy process error: " + ExceptionUtils.getStackTrace(t));
            }
        }
    }

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

    public static Long getCurrentJvmProcessId() {
        // Windows: pid@hostname, for example "1234@ponfee"
        // Unix/Linux: 1234
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return Long.parseLong(name.split("@")[0]);
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
                long handlePointer = ((Number) FieldUtils.readField(process, "handle", true)).longValue();
                WinNT.HANDLE handle = new WinNT.HANDLE();
                handle.setPointer(Pointer.createConstant(handlePointer));
                return (long) Kernel32.INSTANCE.GetProcessId(handle);
            } else if (SystemUtils.IS_OS_UNIX) {
                // Unix Process class：java.lang.UNIXProcess
                return ((Number) FieldUtils.readField(process, "pid", true)).longValue();
            } else {
                LOG.error("Get process id unknown os name: {}, {}", SystemUtils.OS_NAME, process.getClass().getName());
                return null;
            }
        } catch (Throwable t) {
            LOG.error("Get process id error.", t);
            return null;
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
                if (LOG.isInfoEnabled()) {
                    LOG.info("Stop windows process verbose: {}, {}", pid, processVerbose(killProcess, charset));
                }
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
                if (LOG.isInfoEnabled()) {
                    LOG.info("Stop unix process verbose: {}, {}", pid, processVerbose(killProcess, charset));
                }
                destroy(killProcess);
            } else {
                LOG.error("Stop process id unknown os name: {}, {}", SystemUtils.OS_NAME, pid);
            }
        } catch (InterruptedException e) {
            LOG.error("Kill process id '{}' interrupted.", pid);
            ExceptionUtils.rethrow(e);
        } catch (Throwable t) {
            LOG.error("Kill process id '" + pid + "' error.", t);
        }
    }

    public static ExecutionResult completeProcess(Process process, Charset charset, ExecutionTask task, Logger log) {
        try (InputStream is = process.getInputStream(); InputStream es = process.getErrorStream()) {
            // 一次性获取全部执行结果信息：不是在控制台实时展示执行信息，所以此处不用通过异步线程去获取命令的实时执行信息
            String verbose = IOUtils.toString(is, charset);
            String error = IOUtils.toString(es, charset);
            int code = process.waitFor();
            if (code == ProcessUtils.SUCCESS_CODE) {
                return ExecutionResult.success(verbose);
            } else {
                return ExecutionResult.failure(JobCodeMsg.JOB_EXECUTE_FAILED.getCode(), code + ": " + error);
            }
        } catch (Throwable t) {
            log.error("Process execute error: " + task, t);
            Threads.interruptIfNecessary(t);
            return ExecutionResult.failure(JobCodeMsg.JOB_EXECUTE_ERROR.getCode(), Throwables.getRootCauseMessage(t));
        } finally {
            ProcessUtils.destroy(process);
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
