/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.base;

import cn.ponfee.scheduler.common.base.Startable;
import cn.ponfee.scheduler.common.concurrent.Threads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The abstract heartbeat thread.
 *
 * @author Ponfee
 */
public abstract class AbstractHeartbeatThread extends Thread implements Startable {

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
        super.setName(getClass().getSimpleName());
    }

    /**
     * Runnable for thread.
     */
    @Override
    public final void run() {
        log.info("Heartbeat started.");

        while (!stopped.get()) {
            if (super.isInterrupted()) {
                log.warn("Thread exit by interrupted.");
                toStop();
                return;
            }

            boolean isBusyLoop;
            long begin = System.currentTimeMillis();

            try {
                // true is busy loop
                isBusyLoop = heartbeat();
            } catch (Exception e) {
                isBusyLoop = true;
                log.error("Heartbeat occur error, stopped=" + stopped, e);
            }

            long end = System.currentTimeMillis();
            if (log.isDebugEnabled()) {
                log.debug("Heartbeat processed time: {}", end - begin);
            }

            // if busy loop, need sleep a moment
            if (isBusyLoop) {
                // gap period milliseconds(with fixed delay)
                long sleepTimeMillis = heartbeatPeriodMs - (end % heartbeatPeriodMs);
                try {
                    TimeUnit.MILLISECONDS.sleep(sleepTimeMillis);
                    log.debug("Heartbeat slept time: {}", sleepTimeMillis);
                } catch (InterruptedException e) {
                    log.error("Sleep occur error in loop, stopped=" + stopped, e);
                    Thread.currentThread().interrupt();
                    if (stopped.get()) {
                        log.info("Thread exit by stopped.");
                        return;
                    }
                }
            }
        }

        toStop();
        log.info("Heartbeat end.");
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
    public boolean doStop(long joinMillis) {
        toStop();

        int count = 10;
        return Threads.stopThread(this, count, heartbeatPeriodMs / count, joinMillis);
    }

    /**
     * Provide custom implementation for subclass.
     *
     * @return {@code true} if busy loop, need sleep period time.
     */
    protected abstract boolean heartbeat();

}
