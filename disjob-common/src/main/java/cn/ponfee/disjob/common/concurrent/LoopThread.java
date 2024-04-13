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

import cn.ponfee.disjob.common.base.TripState;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loop thread
 *
 * @author Ponfee
 */
public class LoopThread extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(LoopThread.class);

    private final TripState state = TripState.create();
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
        super.setUncaughtExceptionHandler(new LoggedUncaughtExceptionHandler(LOG));
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
            ThrowingRunnable.doChecked(() -> Thread.sleep(delayMs));
        }
        while (state.isRunning()) {
            try {
                action.run();
                Thread.sleep(periodMs);
            } catch (InterruptedException e) {
                LOG.warn("Loop process thread interrupted {}: {}", super.getName(), e.getMessage());
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
        if (state.start()) {
            super.start();
        } else {
            throw new IllegalStateException("Loop process thread already started.");
        }
    }

    public boolean terminate() {
        if (state.stop()) {
            ThrowingRunnable.doCaught(super::interrupt);
            return true;
        } else {
            return false;
        }
    }

}
