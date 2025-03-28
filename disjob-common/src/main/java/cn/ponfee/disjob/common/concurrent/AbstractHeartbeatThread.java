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

import com.google.common.base.CaseFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The abstract heartbeat thread.
 *
 * @author Ponfee
 */
public abstract class AbstractHeartbeatThread extends Thread implements Closeable {

    private static final long MILLIS_PER_SECOND = 1000;
    private static final int MAX_PROCESSED_COUNT = 17;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Thread heartbeat state
     */
    private final TripleState state = TripleState.createStarted();

    /**
     * Heartbeat period milliseconds.
     */
    protected final long heartbeatPeriodMs;

    protected AbstractHeartbeatThread(long heartbeatPeriodMs) {
        log.info("Heartbeat thread init.");
        this.heartbeatPeriodMs = Math.floorDiv(2 * heartbeatPeriodMs, 3);

        // init thread parameters
        super.setDaemon(true);
        super.setName(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, getClass().getSimpleName()) + "_thread");
        super.setPriority(Thread.MAX_PRIORITY);
        super.setUncaughtExceptionHandler(new LoggedUncaughtExceptionHandler(log));
    }

    /**
     * Runnable for thread.
     */
    @Override
    public final void run() {
        log.info("Heartbeat thread started.");

        try {
            int processedCount = 0;
            while (state.isRunning()) {
                if (super.isInterrupted()) {
                    log.error("Heartbeat interrupted state.");
                    break;
                }

                boolean isBusyLoop;
                long begin = System.currentTimeMillis();

                try {
                    // true is busy loop
                    isBusyLoop = heartbeat();
                } catch (InterruptedException e) {
                    log.error("Heartbeat interrupted exception.", e);
                    break;
                } catch (Throwable t) {
                    isBusyLoop = true;
                    log.error("Heartbeat occur error: state=" + state, t);
                }

                long end = System.currentTimeMillis();
                if (log.isDebugEnabled()) {
                    log.debug("Heartbeat processed time: {}", end - begin);
                }

                // if busy loop, need sleep a moment
                if (isBusyLoop) {
                    processedCount = 0;
                    doSleep(heartbeatPeriodMs + ThreadLocalRandom.current().nextLong(heartbeatPeriodMs));
                } else if (++processedCount > MAX_PROCESSED_COUNT) {
                    processedCount = 0;
                    long sleepTime = end % MILLIS_PER_SECOND;
                    sleepTime = sleepTime != 0 ? sleepTime : MILLIS_PER_SECOND;
                    log.info("Max processed count, will sleep time milliseconds: {}", sleepTime);
                    doSleep(sleepTime);
                }
            }
        } catch (InterruptedException e) {
            log.warn("Heartbeat sleep interrupted: state={}, error={}", state, e.getMessage());
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
        doStop(2000);
    }

    public boolean toStop() {
        return state.stop();
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
     * @throws Exception if occur exception
     */
    protected abstract boolean heartbeat() throws Exception;

}
