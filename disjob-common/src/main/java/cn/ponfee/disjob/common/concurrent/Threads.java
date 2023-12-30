/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;

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
     * @param run the runnable
     * @return thread instance
     */
    public static Thread newThread(Runnable run) {
        Thread thread = new Thread(run);
        String callerClassName = Thread.currentThread().getStackTrace()[2].getClassName();
        thread.setName(callerClassName.substring(callerClassName.lastIndexOf(".") + 1));
        thread.setUncaughtExceptionHandler(LoggedUncaughtExceptionHandler.INSTANCE);
        return thread;
    }

    /**
     * New thread
     *
     * @param name     the thread name
     * @param daemon   the daemon
     * @param priority the priority
     * @param run      the runnable
     * @return thread instance
     */
    public static Thread newThread(String name, boolean daemon, int priority, Runnable run) {
        Thread thread = new Thread(run);
        thread.setName(name);
        thread.setDaemon(daemon);
        thread.setPriority(priority);
        thread.setUncaughtExceptionHandler(LoggedUncaughtExceptionHandler.INSTANCE);
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
     * @param thread      the thread
     * @param joinMillis  the joinMillis
     */
    public static void stopThread(Thread thread, long joinMillis) {
        stopThread(thread, joinMillis, false);
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

    // ------------------------------------------------------------private methods

    private static String buildStackTrace(StackTraceElement[] traces) {
        StringBuilder builder = new StringBuilder();
        for (int i = 2, n = traces.length; i < n; i++) {
            builder.append("--\t").append(traces[i].toString()).append("\n");
        }
        return builder.toString();
    }

    private static void stopThread(Thread thread, long joinMillis, boolean fromAsync) {
        if (isStopped(thread)) {
            return;
        }

        thread.interrupt();

        if (Thread.currentThread() == thread) {
            if (fromAsync) {
                LOG.warn("Call stop on self thread: {}\n{}", thread.getName(), getStackTrace());
                stopThread(thread);
            } else {
                ForkJoinPool.commonPool().execute(() -> stopThread(thread, Math.max(joinMillis, 5), true));
            }
            return;
        }

        // wait joined
        if (joinMillis > 0) {
            try {
                thread.join(joinMillis);
            } catch (InterruptedException e) {
                LOG.error("Join thread terminal interrupted: " + thread.getName(), e);
                Thread.currentThread().interrupt();
            }
        }

        stopThread(thread);
    }

    /**
     * Stop the thread
     *
     * @param thread the thread
     */
    private static void stopThread(Thread thread) {
        if (isStopped(thread)) {
            return;
        }

        thread.interrupt();
        try {
            // 调用后，thread中正在执行的run方法内部会抛出java.lang.ThreadDeath异常
            // 如果在run方法内用 try{...} catch(Throwable e){} 捕获住，则线程不会停止执行
            thread.stop();
            LOG.info("Invoke java.lang.Thread#stop() method finished: {}", thread.getName());
        } catch (Throwable t) {
            LOG.error("Invoke java.lang.Thread#stop() method failed: " + thread.getName(), t);
        }
    }

}
