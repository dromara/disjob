package cn.ponfee.scheduler.core.base;

import cn.ponfee.scheduler.common.concurrent.MultithreadExecutors;
import cn.ponfee.scheduler.common.date.WrappedFastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Stop operate flag.
     */
    private final AtomicBoolean stop = new AtomicBoolean(false);

    /**
     * Stop state, {@code true} has been stopped.
     */
    private volatile boolean stopped = false;

    /**
     * Heartbeat interval milliseconds.
     */
    private final long interval;

    public AbstractHeartbeatThread(int heartbeatIntervalSeconds) {
        logger.info("Heartbeat thread init {}", this.getClass());
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
        logger.info("Heartbeat started.");

        try {
            Thread.sleep(MILLIS_PER_SECOND - (System.currentTimeMillis() % MILLIS_PER_SECOND));
        } catch (InterruptedException e) {
            logger.error("Sleep occur error at starting, stopped=" + stopped, e);
            Thread.currentThread().interrupt();
            if (stopped) {
                return;
            }
        }

        while (!stopped) {
            if (super.isInterrupted()) {
                logger.warn("Thread interrupted.");
                stopped = true;
                return;
            }

            boolean status;
            long start = System.currentTimeMillis();
            if (logger.isDebugEnabled()) {
                logger.debug("Heartbeat round date time: {}", WrappedFastDateFormat.PATTERN_51.format(new Date(start)));
            }
            try {
                // true is busy loop
                status = heartbeat();
            } catch (Exception e) {
                status = false;
                logger.error("Heartbeat occur error, stopped=" + stopped, e);
            }

            long end = System.currentTimeMillis();
            if (status) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Heartbeat not do sleep, cost: {}", end - start);
                }
            } else {
                // gap interval milliseconds
                long sleepTimeMillis = interval - (end % MILLIS_PER_SECOND);
                if (logger.isDebugEnabled()) {
                    logger.debug("Heartbeat will sleep time: {}", sleepTimeMillis);
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(sleepTimeMillis);
                } catch (InterruptedException e) {
                    logger.error("Sleep occur error in loop, stopped=" + stopped, e);
                    Thread.currentThread().interrupt();
                    if (stopped) {
                        return;
                    }
                }
            }
        }

        stopped = true;
        logger.info("Heartbeat end.");
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
        return MultithreadExecutors.isStopped(this);
    }

    @Override
    protected void finalize() {
        doStop(0);
    }

    @Override
    public void close() {
        doStop(MILLIS_PER_SECOND);
    }

    public void toStop() {
        stopped = true;
    }

    /**
     * Stop heartbeat.
     *
     * @param joinMillis the join milliseconds
     */
    public boolean doStop(long joinMillis) {
        toStop();
        if (!stop.compareAndSet(false, true)) {
            logger.warn("Repeat do stop thread: {}", this.getName());
            return false;
        }

        int count = 10;
        return MultithreadExecutors.stopThread(
            this, count, interval / count, joinMillis
        );
    }

    /**
     * Provide custom implementation for subclass.
     *
     * @return {@code true} if busy loop, need sleep interval time.
     */
    protected abstract boolean heartbeat();

}
