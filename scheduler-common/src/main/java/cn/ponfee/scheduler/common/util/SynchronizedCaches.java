package cn.ponfee.scheduler.common.util;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Synchronized cache
 *
 * @author Ponfee
 */
public final class SynchronizedCaches {

    public static <K, V> V get(K key, Map<K, V> cache, Function<K, V> mapper) {
        V val = cache.get(key);
        if (val != null) {
            return val;
        }
        synchronized (cache) {
            if ((val = cache.get(key)) == null) {
                if ((val = mapper.apply(key)) != null) {
                    cache.put(key, val);
                }
            }
        }
        return val;
    }

    public static <K, V> V get(K key, Map<K, V> cache, Supplier<V> supplier) {
        V val = cache.get(key);
        if (val != null) {
            return val;
        }
        synchronized (cache) {
            if ((val = cache.get(key)) == null) {
                if ((val = supplier.get()) != null) {
                    cache.put(key, val);
                }
            }
        }
        return val;
    }
}
