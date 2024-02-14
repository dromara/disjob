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

import com.google.common.primitives.Ints;

import java.util.Objects;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Delayed data
 *
 * @author Ponfee
 */
public class DelayedData<E> implements Delayed {

    private final long fireTime;
    private final E data;

    private DelayedData(E data, long delayInMilliseconds) {
        this.data = Objects.requireNonNull(data);
        this.fireTime = System.currentTimeMillis() + delayInMilliseconds;
    }

    public static <E> DelayedData<E> of(E data, long delayInMilliseconds) {
        return new DelayedData<>(data, delayInMilliseconds);
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long diff = fireTime - System.currentTimeMillis();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        return Ints.saturatedCast(this.fireTime - ((DelayedData<E>) o).fireTime);
    }

    public E getData() {
        return data;
    }

}
