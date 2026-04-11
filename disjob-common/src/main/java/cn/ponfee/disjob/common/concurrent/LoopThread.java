/*
 * Copyright 2022-2026 Ponfee (http://www.ponfee.cn/)
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

import cn.ponfee.disjob.common.exception.Throwables;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import lombok.extern.slf4j.Slf4j;

/**
 * Loop thread
 *
 * @author Ponfee
 */
@Slf4j
public class LoopThread extends Thread {

    private final TripleState state = TripleState.create();
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
        super.setUncaughtExceptionHandler(new LoggedUncaughtExceptionHandler(log));
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
        log.info("Loop thread begin.");
        if (delayMs > 0) {
            Threads.sleep(delayMs);
        }
        while (state.isRunning()) {
            try {
                action.run();
                // noinspection BusyWait
                Thread.sleep(periodMs);
            } catch (Throwable t) {
                if (Throwables.isFatal(t)) {
                    terminate();
                    log.warn("Loop thread terminated {}: {}({})", super.getName(), t.getClass().getName(), t.getMessage());
                } else {
                    log.error("Loop thread error: {}", super.getName(), t);
                }
            }
        }
        log.info("Loop thread end.");
    }

    @Override
    public synchronized void start() {
        if (state.start()) {
            super.start();
        } else {
            throw new IllegalStateException("Loop thread start failed, current state: " + state);
        }
    }

    public boolean terminate() {
        boolean result = state.stop();
        if (result) {
            // interrupt this thread sleep in run method
            ThrowingRunnable.doCaught(super::interrupt);
        }
        return result;
    }

}
