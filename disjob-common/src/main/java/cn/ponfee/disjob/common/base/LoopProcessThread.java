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
 * Loop process thread
 *
 * @author Ponfee
 */
public class LoopProcessThread extends Thread {

    private final static Logger LOG = LoggerFactory.getLogger(LoopProcessThread.class);

    private static final int NEW = 0;
    private static final int RUNNABLE = 1;
    private static final int TERMINATED = 2;

    private final AtomicInteger state = new AtomicInteger(NEW);
    private final long periodMs;
    private final long delayMs;
    private final ThrowingRunnable<?> action;

    public LoopProcessThread(String name, long periodMs, long delayMs, ThrowingRunnable<?> action) {
        this(name, true, Thread.MAX_PRIORITY, periodMs, delayMs, action);
    }

    public LoopProcessThread(String name, boolean daemon, int priority,
                             long periodMs, long delayMs, ThrowingRunnable<?> action) {
        super.setName(name);
        super.setDaemon(daemon);
        super.setPriority(priority);
        super.setUncaughtExceptionHandler(LoggedUncaughtExceptionHandler.INSTANCE);
        this.periodMs = periodMs;
        this.delayMs = delayMs;
        this.action = action;
    }

    @Override
    public void run() {
        LOG.info("Loop process thread begin.");
        if (delayMs > 0) {
            ThrowingRunnable.checked(() -> Thread.sleep(delayMs));
        }
        while (state.get() == RUNNABLE) {
            try {
                action.run();
                Thread.sleep(periodMs);
            } catch (InterruptedException e) {
                LOG.error("Loop process thread interrupted.", e);
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
        if (state.compareAndSet(NEW, RUNNABLE)) {
            super.start();
        } else {
            throw new IllegalStateException("Loop process thread already started.");
        }
    }

    public boolean terminate() {
        if (state.compareAndSet(RUNNABLE, TERMINATED)) {
            ThrowingRunnable.execute(super::interrupt);
            return true;
        } else {
            return false;
        }
    }

}
