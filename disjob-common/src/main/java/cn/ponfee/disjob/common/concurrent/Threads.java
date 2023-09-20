/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.concurrent;

import cn.ponfee.disjob.common.base.LoggedUncaughtExceptionHandler;
import cn.ponfee.disjob.common.util.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * @param sleepCount  the sleepCount
     * @param sleepMillis the sleepMillis
     * @param joinMillis  the joinMillis
     */
    public static void stopThread(Thread thread, int sleepCount, long sleepMillis, long joinMillis) {
        if (isStopped(thread)) {
            return;
        }

        if (Thread.currentThread() == thread) {
            LOG.warn("Call stop on self thread: {}\n{}", thread.getName(), ObjectUtils.getStackTrace());
            thread.interrupt();
            stopThread(thread);
        }

        // sleep for wait the tread run method block code execute finish
        LOG.info("Thread stopping: {}", thread.getName());
        while (sleepCount-- > 0 && sleepMillis > 0 && !isStopped(thread)) {
            try {
                // Wait some time
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                LOG.error("Waiting thread terminal interrupted: " + thread.getName(), e);
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (isStopped(thread)) {
            return;
        }

        // interrupt and wait joined
        thread.interrupt();
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

    public static void interruptIfNecessary(Throwable t) {
        if (t instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
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

        try {
            // 调用后，thread中正在执行的run方法内部会抛出java.lang.ThreadDeath异常
            // 如果在run方法内用 try{...} catch(Throwable e){} 捕获住，则线程不会停止执行
            thread.stop();
            LOG.info("Invoked java.lang.Thread#stop() method: {}", thread.getName());
        } catch (Throwable t) {
            LOG.error("Invoke java.lang.Thread#stop() method failed: " + thread.getName(), t);
        }
    }

}
