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

import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
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

    public static String getName(Thread thread) {
        return thread == null ? null : thread.getName();
    }

    /**
     * New thread
     *
     * @param name     the thread name
     * @param daemon   the daemon
     * @param priority the priority
     * @param runnable the runnable
     * @param logger   the uncaught exception handler logger
     * @return thread instance
     */
    public static Thread newThread(String name, boolean daemon, int priority, Runnable runnable, Logger logger) {
        Thread thread = new Thread(runnable);
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
        LOG.info("Stop thread start [{}]", thread.getName());
        stopThread0(thread, joinMillis);
        LOG.info("Stop thread end [{}]", thread.getName());
    }

    public static void interruptIfNecessary(Throwable t) {
        if (t instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 获取栈帧
     *
     * @param depth the depth
     * @return stack trace
     */
    public static String getStackFrame(int depth) {
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
                    ThrowingRunnable.doCaught(() -> Thread.sleep(sleepTime));
                } else {
                    ThrowingRunnable.doChecked(() -> Thread.sleep(sleepTime));
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

    private static void stopThread0(Thread thread, long joinMillis) {
        if (thread == Thread.currentThread()) {
            LOG.info("Stop self thread [{}]\n{}", thread.getName(), getStackTrace());
            return;
        }
        if (isStopped(thread)) {
            LOG.info("Thread already stopped [{}]", thread.getName());
            return;
        }

        long halfJoinMillis = joinMillis / 2;

        // wait joined
        if (join(thread, halfJoinMillis)) {
            return;
        }

        // again wait joined with interrupt
        thread.interrupt();
        if (join(thread, halfJoinMillis)) {
            return;
        }

        try {
            // 调用后，thread中正在执行的run方法内部会抛出java.lang.ThreadDeath异常
            // 如果在run方法内用 try{...} catch(Throwable e){} 捕获住，则线程不会停止执行
            LOG.warn("Invoke java.lang.Thread#stop() method begin: {}", thread.getName());
            thread.stop();
            LOG.warn("Invoke java.lang.Thread#stop() method end: {}", thread.getName());
        } catch (Throwable t) {
            LOG.error("Invoke java.lang.Thread#stop() method failed: " + thread.getName(), t);
        }
    }

    private static boolean join(Thread thread, long joinTimeoutMills) {
        if (joinTimeoutMills > 0) {
            try {
                thread.join(joinTimeoutMills);
            } catch (Throwable e) {
                LOG.error("Join thread terminal interrupted: " + thread.getName(), e);
                interruptIfNecessary(e);
            }
        }
        return isStopped(thread);
    }

}
