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

package cn.ponfee.disjob.dispatch.route.count;

/**
 * Atomic counter
 *
 * @author Ponfee
 */
public abstract class AtomicCounter {

    /**
     * Gets the current count value.
     *
     * @return current count value
     */
    public abstract long get();

    /**
     * Sets a new value.
     *
     * @param newValue newly value
     */
    public abstract void set(long newValue);

    /**
     * Add specified delta number and get the new value.
     *
     * @param delta the number of delta
     * @return newly value
     */
    public abstract long addAndGet(long delta);

    /**
     * Gets the current value and then add one.
     *
     * @return current value
     */
    public final long getAndIncrement() {
        return getAndAdd(1);
    }

    /**
     * Add one and get the new value.
     *
     * @return newly value
     */
    public final long incrementAndGet() {
        return addAndGet(1);
    }

    /**
     * Gets the current value and then add delta number.
     *
     * @return current value
     */
    public final long getAndAdd(long delta) {
        return addAndGet(delta) - delta;
    }

}
