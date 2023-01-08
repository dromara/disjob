/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.base;

import cn.ponfee.scheduler.common.concurrent.Threads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static cn.ponfee.scheduler.common.date.JavaUtilDateFormat.PATTERN_51;

/**
 * The abstract heartbeat thread.
 *
 * @author Ponfee
 */
public abstract class AbstractHeartbeatThread extends Thread implements AutoCloseable {

    /**
     * Number of milliseconds per second.
     */
    private static final long MILLIS_PER_SECOND = 1000;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Thread is whether stopped status
     */
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    /**
     * Heartbeat interval milliseconds.
     */
    private final long interval;

    public AbstractHeartbeatThread(int heartbeatIntervalSeconds) {
        log.info("Heartbeat thread init {}", this.getClass());
        this.interval = TimeUnit.SECONDS.toMillis(heartbeatIntervalSeconds);

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

        try {
            Thread.sleep(MILLIS_PER_SECOND - (System.currentTimeMillis() % MILLIS_PER_SECOND));
        } catch (InterruptedException e) {
            log.error("Sleep occur error at starting, stopped=" + stopped, e);
            Thread.currentThread().interrupt();
            if (stopped.get()) {
                return;
            }
        }

        while (!stopped.get()) {
            if (super.isInterrupted()) {
                log.warn("Thread interrupted.");
                stopped.compareAndSet(false, true);
                return;
            }

            boolean result;
            long start = System.currentTimeMillis();
            if (log.isDebugEnabled()) {
                log.debug("Heartbeat round date time: {}", PATTERN_51.format(new Date(start)));
            }
            try {
                // true is busy loop
                result = heartbeat();
            } catch (Exception e) {
                result = false;
                log.error("Heartbeat occur error, stopped=" + stopped, e);
            }

            long end = System.currentTimeMillis();
            if (result) {
                if (log.isDebugEnabled()) {
                    log.debug("Heartbeat not do sleep, cost: {}", end - start);
                }
            } else {
                // gap interval milliseconds
                long sleepTimeMillis = interval - (end % MILLIS_PER_SECOND);
                if (log.isDebugEnabled()) {
                    log.debug("Heartbeat will sleep time: {}", sleepTimeMillis);
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(sleepTimeMillis);
                } catch (InterruptedException e) {
                    log.error("Sleep occur error in loop, stopped=" + stopped, e);
                    Thread.currentThread().interrupt();
                    if (stopped.get()) {
                        return;
                    }
                }
            }
        }

        stopped.compareAndSet(false, true);
        log.info("Heartbeat end.");
    }

    /**
     * Returns interval milliseconds.
     *
     * @return interval milliseconds
     */
    public final long interval() {
        return interval;
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
        doStop(MILLIS_PER_SECOND);
    }

    public void toStop() {
        stopped.compareAndSet(false, true);
    }

    /**
     * Stop heartbeat.
     *
     * @param joinMillis the join milliseconds
     */
    public boolean doStop(long joinMillis) {
        toStop();
        if (!stopped.compareAndSet(false, true)) {
            log.warn("Repeat do stop thread: {}", this.getName());
            return false;
        }

        int count = 10;
        return Threads.stopThread(this, count, interval / count, joinMillis);
    }

    /**
     * Provide custom implementation for subclass.
     *
     * @return {@code true} if busy loop, need sleep interval time.
     */
    protected abstract boolean heartbeat();

}
