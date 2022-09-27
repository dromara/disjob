package cn.ponfee.scheduler.common.base;

import java.util.Map;

/**
 * Get the value with typed for {@link Map}
 * 
 * @author Ponfee
 * @param <K> the key type
 * @param <V> the value type
 */
public interface TypedMap<K, V> extends Map<K, V>, TypedKeyValue<K, V> {

    @Override
    default V getValue(K key) {
        return this.get(key);
    }

    @Override
    default V removeKey(K key) {
        return this.remove(key);
    }

}
