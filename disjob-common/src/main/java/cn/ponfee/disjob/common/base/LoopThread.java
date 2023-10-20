/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.base;

import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Loop thread
 *
 * @author Ponfee
 */
public class LoopThread extends Thread {

    private final static Logger LOG = LoggerFactory.getLogger(LoopThread.class);

    private static final int NEW        = 0;
    private static final int RUNNING    = 1;
    private static final int TERMINATED = 2;

    private final AtomicInteger state = new AtomicInteger(NEW);
    private final long periodMs;
    private final long delayMs;
    private final ThrowingRunnable<?> action;

    public LoopThread(String name, long periodMs, long delayMs, ThrowingRunnable<?> action) {
        this(name, true, Thread.MAX_PRIORITY, periodMs, delayMs, action);
    }

    public LoopThread(String name, boolean daemon, int priority,
                      long periodMs, long delayMs, ThrowingRunnable<?> action) {
        super.setName(name);
        super.setDaemon(daemon);
        super.setPriority(priority);
        super.setUncaughtExceptionHandler(LoggedUncaughtExceptionHandler.INSTANCE);
        this.periodMs = periodMs;
        this.delayMs = delayMs;
        this.action = action;
    }

    public static LoopThread createStarted(String name, long periodMs, long delayMs, ThrowingRunnable<?> action) {
        return createStarted(name, true, Thread.MAX_PRIORITY, periodMs, delayMs, action);
    }

    public static LoopThread createStarted(String name, boolean daemon, int priority,
                                           long periodMs, long delayMs, ThrowingRunnable<?> action) {
        LoopThread thread = new LoopThread(name, daemon, priority, periodMs, delayMs, action);
        thread.start();
        return thread;
    }

    @Override
    public void run() {
        LOG.info("Loop process thread begin.");
        if (delayMs > 0) {
            ThrowingRunnable.checked(() -> Thread.sleep(delayMs));
        }
        while (state.get() == RUNNING) {
            try {
                action.run();
                Thread.sleep(periodMs);
            } catch (InterruptedException e) {
                LOG.warn("Loop process thread interrupted: {}", e.getMessage());
                terminate();
                Thread.currentThread().interrupt();
            } catch (Throwable e) {
                LOG.error("Loop process thread error.", e);
            }
        }
        LOG.info("Loop process thread end.");
    }

    @Override
    public synchronized void start() {
        if (state.compareAndSet(NEW, RUNNING)) {
            super.start();
        } else {
            throw new IllegalStateException("Loop process thread already started.");
        }
    }

    public boolean terminate() {
        if (state.compareAndSet(RUNNING, TERMINATED)) {
            ThrowingRunnable.execute(super::interrupt);
            return true;
        } else {
            return false;
        }
    }

}
