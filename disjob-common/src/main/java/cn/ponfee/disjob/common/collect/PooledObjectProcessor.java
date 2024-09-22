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

package cn.ponfee.disjob.common.collect;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Pooled object processor
 *
 * @author Ponfee
 */
public class PooledObjectProcessor<K, V> {

    private static final Logger LOG = LoggerFactory.getLogger(PooledObjectProcessor.class);

    private final ConcurrentMap<K, SubPool> pool = new ConcurrentHashMap<>();
    private final int size;
    private final Function<K, V> creator;

    public PooledObjectProcessor(int size, Function<K, V> creator) {
        this.size = size;
        this.creator = creator;
    }

    public <R> R process(K key, Function<V, R> function) throws InterruptedException {
        SubPool subPool = pool.computeIfAbsent(key, SubPool::new);
        V value = subPool.borrowObject();
        try {
            return function.apply(value);
        } finally {
            subPool.returnObject(value);
        }
    }

    private class SubPool {
        final K key;
        final BlockingQueue<V> queue;
        // 剩余可创建的数量
        final AtomicInteger counter;

        SubPool(K key) {
            this.key = key;
            this.queue = new ArrayBlockingQueue<>(size);
            this.counter = new AtomicInteger(size);
        }

        V borrowObject() throws InterruptedException {
            V value = queue.poll();
            if (value != null) {
                return value;
            }

            for (; ; ) {
                if (requiredCreate()) {
                    try {
                        value = creator.apply(key);
                    } catch (Throwable e) {
                        counter.incrementAndGet();
                        return ExceptionUtils.rethrow(e);
                    }
                    if (value != null) {
                        LOG.debug("Created new object.");
                        return value;
                    } else {
                        counter.incrementAndGet();
                        throw new NullPointerException("Created null object: " + key);
                    }
                }
                if ((value = queue.poll(200L, TimeUnit.MILLISECONDS)) != null) {
                    return value;
                }
            }
        }

        void returnObject(V value) throws InterruptedException {
            queue.put(Objects.requireNonNull(value));
        }

        boolean requiredCreate() {
            for (int count; (count = counter.get()) > 0; ) {
                if (counter.compareAndSet(count, count - 1)) {
                    return true;
                }
            }
            return false;
        }
    }

}
