/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.concurrent;

import cn.ponfee.disjob.common.base.LoggedUncaughtExceptionHandler;
import com.google.common.base.CaseFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The abstract heartbeat thread.
 *
 * @author Ponfee
 */
public abstract class AbstractHeartbeatThread extends Thread implements Closeable {

    private static final long MILLIS_PER_SECOND = 1000;
    private static final int MAX_PROCESSED_COUNT = 997;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Thread is whether stopped status
     */
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    /**
     * Heartbeat period milliseconds.
     */
    protected final long heartbeatPeriodMs;

    public AbstractHeartbeatThread(long heartbeatPeriodMs) {
        log.info("Heartbeat thread init {}", getClass());
        this.heartbeatPeriodMs = heartbeatPeriodMs;

        // init thread parameters
        super.setDaemon(true);
        super.setName(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, getClass().getSimpleName()) + "_thread");
        super.setPriority(Thread.MAX_PRIORITY);
        super.setUncaughtExceptionHandler(LoggedUncaughtExceptionHandler.INSTANCE);
    }

    /**
     * Runnable for thread.
     */
    @Override
    public final void run() {
        log.info("Heartbeat thread started.");

        try {
            int processedCount = 0;
            while (!stopped.get()) {
                if (super.isInterrupted()) {
                    log.error("Thread exit by interrupted.");
                    break;
                }

                boolean isBusyLoop;
                long begin = System.currentTimeMillis();

                try {
                    // true is busy loop
                    isBusyLoop = heartbeat();
                } catch (Throwable t) {
                    isBusyLoop = true;
                    log.error("Heartbeat occur error, stopped=" + stopped, t);
                }

                long end = System.currentTimeMillis();
                if (log.isDebugEnabled()) {
                    log.debug("Heartbeat processed time: {}", end - begin);
                }

                // if busy loop, need sleep a moment
                if (isBusyLoop) {
                    processedCount = 0;
                    // gap period milliseconds(with fixed delay)
                    doSleep(heartbeatPeriodMs);
                } else if (++processedCount > MAX_PROCESSED_COUNT) {
                    doSleep(MILLIS_PER_SECOND - (end % MILLIS_PER_SECOND));
                }
            }
        } catch (InterruptedException e) {
            log.warn("Sleep occur error in loop, stopped={}, error={}", stopped, e.getMessage());
            Thread.currentThread().interrupt();
        }

        toStop();
        log.info("Heartbeat end.");
    }

    private void doSleep(long sleepTimeMillis) throws InterruptedException {
        Thread.sleep(sleepTimeMillis);
        log.debug("Heartbeat sleep time: {}", sleepTimeMillis);
    }

    /**
     * Returns thread is whether stopped
     *
     * @return {@code true} if stopped.
     */
    public final boolean isStopped() {
        return Threads.isStopped(this);
    }

    @Override
    public void close() {
        doStop(1000);
    }

    public boolean toStop() {
        return stopped.compareAndSet(false, true);
    }

    /**
     * Stop heartbeat.
     *
     * @param joinMillis the join milliseconds
     */
    public void doStop(long joinMillis) {
        toStop();

        Threads.stopThread(this, joinMillis);
    }

    /**
     * Provide custom implementation for subclass.
     *
     * @return {@code true} if busy loop, need sleep period time.
     */
    protected abstract boolean heartbeat();

}
