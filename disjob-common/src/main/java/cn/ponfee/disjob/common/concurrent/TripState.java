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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Trip state: New -> Running -> Stopped
 *
 * @author Ponfee
 */
public class TripState {

    private static final int NEW     = 0;
    private static final int RUNNING = 1;
    private static final int STOPPED = 2;

    private final AtomicInteger state;

    private TripState() {
        this.state = new AtomicInteger(NEW);
    }

    public static TripState create() {
        return new TripState();
    }

    public static TripState createStarted() {
        TripState state = new TripState();
        state.start();
        return state;
    }

    public boolean isNew() {
        return state.get() == NEW;
    }

    public boolean start() {
        return state.compareAndSet(NEW, RUNNING);
    }

    public boolean isRunning() {
        return state.get() == RUNNING;
    }

    public boolean stop() {
        return state.compareAndSet(RUNNING, STOPPED);
    }

    public boolean isStopped() {
        return state.get() == STOPPED;
    }

    @Override
    public String toString() {
        if (isNew()) {
            return "New";
        }
        return isRunning() ? "Running" : "Stopped";
    }

}
