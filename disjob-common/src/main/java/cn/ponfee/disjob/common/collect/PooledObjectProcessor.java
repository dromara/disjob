/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.collect;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Pooled object processor
 *
 * @author Ponfee
 */
public class PooledObjectProcessor<K, V> {

    private static final Logger LOG = LoggerFactory.getLogger(PooledObjectProcessor.class);

    private final Map<K, Wrapper> cache = new ConcurrentHashMap<>();
    private final int size;
    private final Function<K, V> creator;

    public PooledObjectProcessor(int size, Function<K, V> creator) {
        this.size = size;
        this.creator = creator;
    }

    public <R> R process(K key, Function<V, R> function) throws InterruptedException {
        Wrapper wrapper = cache.computeIfAbsent(key, Wrapper::new);
        V value = wrapper.borrowObject();
        try {
            return function.apply(value);
        } finally {
            wrapper.returnObject(value);
        }
    }

    private class Wrapper {
        final K key;
        final BlockingQueue<V> queue;
        final AtomicInteger counter;

        Wrapper(K key) {
            this.key = key;
            this.queue = new ArrayBlockingQueue<>(size);
            this.counter = new AtomicInteger(size);
        }

        V borrowObject() throws InterruptedException {
            V value = queue.poll();
            if (value != null) {
                return value;
            }

            if (!requiredCreate()) {
                LOG.debug("Blocking until object.");
                return queue.take();
            }

            try {
                value = creator.apply(key);
                if (value == null) {
                    counter.incrementAndGet();
                    throw new NullPointerException("Created null object: " + key);
                } else {
                    LOG.debug("Created new object.");
                    return value;
                }
            } catch (Throwable e) {
                counter.incrementAndGet();
                return ExceptionUtils.rethrow(e);
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
