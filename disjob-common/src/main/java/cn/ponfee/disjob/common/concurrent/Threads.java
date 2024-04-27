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

package cn.ponfee.disjob.common.concurrent;

import cn.ponfee.disjob.common.exception.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BooleanSupplier;

/**
 * Thread utilities
 *
 * @author Ponfee
 */
public final class Threads {

    private static final Logger LOG = LoggerFactory.getLogger(Threads.class);

    /**
     * New thread
     *
     * @param name     the thread name
     * @param daemon   the daemon
     * @param priority the priority
     * @param run      the runnable
     * @param logger   the uncaught exception handler logger
     * @return thread instance
     */
    public static Thread newThread(String name, boolean daemon, int priority, Runnable run, Logger logger) {
        Thread thread = new Thread(run);
        thread.setName(name);
        thread.setDaemon(daemon);
        thread.setPriority(priority);
        if (logger != null) {
            thread.setUncaughtExceptionHandler(new LoggedUncaughtExceptionHandler(logger));
        }
        return thread;
    }

    /**
     * Returns the thread is whether stopped
     *
     * @param thread the thread
     * @return {@code true} if the thread is stopped
     */
    public static boolean isStopped(Thread thread) {
        return thread.getState() == Thread.State.TERMINATED;
    }

    /**
     * Stops the thread
     *
     * @param thread     the thread
     * @param joinMillis the joinMillis
     */
    public static void stopThread(Thread thread, long joinMillis) {
        if (thread == null || thread == Thread.currentThread() || isStopped(thread)) {
            return;
        }

        // wait joined
        join(thread, joinMillis / 2);
        if (isStopped(thread)) {
            return;
        }

        // again with interrupt wait joined
        thread.interrupt();
        join(thread, joinMillis / 2);
        if (isStopped(thread)) {
            return;
        }

        try {
            // 调用后，thread中正在执行的run方法内部会抛出java.lang.ThreadDeath异常
            // 如果在run方法内用 try{...} catch(Throwable e){} 捕获住，则线程不会停止执行
            thread.stop();
            LOG.warn("Invoke java.lang.Thread#stop() method finished: {}", thread.getName());
        } catch (Throwable t) {
            LOG.error("Invoke java.lang.Thread#stop() method failed: " + thread.getName(), t);
        }
    }

    public static void interruptIfNecessary(Throwable t) {
        if (t instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 获取堆栈信息
     *
     * @param depth the depth
     * @return stack trace
     */
    public static String getStackTrace(int depth) {
        StackTraceElement[] traces = Thread.currentThread().getStackTrace();
        return depth < traces.length ? traces[depth].toString() : null;
    }

    public static String getStackTrace() {
        return buildStackTrace(Thread.currentThread().getStackTrace());
    }

    public static String getStackTrace(Thread thread) {
        return buildStackTrace(thread.getStackTrace());
    }

    public static boolean waitUntil(int round, long[] sleepMillis, BooleanSupplier supplier) {
        return waitUntil(round, sleepMillis, true, supplier);
    }

    public static boolean waitUntil(int round, long[] sleepMillis, boolean caught, BooleanSupplier supplier) {
        int lastIndex = sleepMillis.length - 1;
        for (int i = 0; i < round; i++) {
            long sleepTime = sleepMillis[Math.min(i, lastIndex)];
            if (sleepTime > 0) {
                if (caught) {
                    Throwables.ThrowingRunnable.doCaught(() -> Thread.sleep(sleepTime));
                } else {
                    Throwables.ThrowingRunnable.doChecked(() -> Thread.sleep(sleepTime));
                }
            }
            if (supplier.getAsBoolean()) {
                return true;
            }
        }

        return false;
    }

    // ------------------------------------------------------------private methods

    private static String buildStackTrace(StackTraceElement[] traces) {
        StringBuilder builder = new StringBuilder();
        for (int i = 2, n = traces.length; i < n; i++) {
            builder.append("\t").append(traces[i].toString()).append("\n");
        }
        if (builder.length() > 0) {
            // delete end with '\n'
            builder.setLength(builder.length() - 1);
        }
        return builder.toString();
    }

    private static void join(Thread thread, long joinTimeoutMills) {
        if (joinTimeoutMills <= 0) {
            return;
        }
        try {
            thread.join(joinTimeoutMills);
        } catch (Throwable e) {
            LOG.error("Join thread terminal interrupted: " + thread.getName(), e);
            interruptIfNecessary(e);
        }
    }

}
