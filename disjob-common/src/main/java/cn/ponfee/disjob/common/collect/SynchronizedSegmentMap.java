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

import cn.ponfee.disjob.common.exception.Throwables.ThrowingConsumer;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingFunction;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Synchronized segment map
 *
 * @author Ponfee
 */
@SuppressWarnings({"SynchronizationOnLocalVariableOrMethodParameter", "unchecked"})
public class SynchronizedSegmentMap<K, V> implements Map<K, V> {

    private static final int MAXIMUM_CAPACITY = 1 << 30;

    private final Map<K, V>[] segments;
    private final int mask;
    private final int bits;
    private final int round;

    public SynchronizedSegmentMap() {
        this(16);
    }

    public SynchronizedSegmentMap(int buckets) {
        Assert.isTrue(1 < buckets && buckets <= 1024, "Segment buckets must be range (1, 1024]");

        this.segments = new Map[segmentBucketsFor(buckets)];
        this.mask = segments.length - 1;
        this.bits = Integer.toBinaryString(mask).length();
        this.round = (Integer.SIZE + bits - 1) / bits;

        for (int i = 0; i < segments.length; i++) {
            segments[i] = new HashMap<>();
        }
    }

    public final <R, E extends Throwable> R process(K key, ThrowingFunction<Map<K, V>, R, E> action) throws E {
        Map<K, V> map = segmentAt(key);
        synchronized (map) {
            return action.apply(map);
        }
    }

    public final <E extends Throwable> void execute(K key, ThrowingConsumer<Map<K, V>, E> action) throws E {
        Map<K, V> map = segmentAt(key);
        synchronized (map) {
            action.accept(map);
        }
    }

    public final <E extends Throwable> void execute(ThrowingConsumer<Map<K, V>, E> action) throws E {
        for (Map<K, V> map : segments) {
            synchronized (map) {
                action.accept(map);
            }
        }
    }

    @Override
    public V put(K key, V value) {
        return process(key, e -> e.put(key, value));
    }

    @Override
    public V remove(Object key) {
        return process((K) key, e -> e.remove(key));
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        m.forEach((k, v) -> execute(k, e -> e.put(k, v)));
    }

    @Override
    public void clear() {
        execute(Map::clear);
    }

    // ----------------------------------------------------------without synchronized

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        for (Map<K, V> map : segments) {
            map.forEach(action);
        }
    }

    @Override
    public int size() {
        int res = 0;
        for (Map<K, V> map : segments) {
            res += map.size();
        }
        return res;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return segmentAt(key).containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        for (Map<K, V> map : segments) {
            if (map.containsValue(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        return segmentAt(key).get(key);
    }

    @Override
    public Set<K> keySet() {
        ImmutableSet.Builder<K> builder = ImmutableSet.builder();
        for (Map<K, V> map : segments) {
            builder.addAll(map.keySet());
        }
        return builder.build();
    }

    @Override
    public Collection<V> values() {
        ImmutableList.Builder<V> builder = ImmutableList.builder();
        for (Map<K, V> map : segments) {
            builder.addAll(map.values());
        }
        return builder.build();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        ImmutableSet.Builder<Entry<K, V>> builder = ImmutableSet.builder();
        for (Map<K, V> map : segments) {
            builder.addAll(map.entrySet());
        }
        return builder.build();
    }

    // ------------------------------------------------------------private methods

    private Map<K, V> segmentAt(Object key) {
        int index = (key == null) ? 0 : calculateIndex(key.hashCode());
        return segments[index];
    }

    private int calculateIndex(int n) {
        int r = n & mask;
        // 0 ^ x = x: 无论x是0还是1，结果都是x，最后一轮不足整数倍时右移后左侧是补0，0与r相应的位置`^`后还是r所在位置的值
        // 如：101101 >>> 4  ->  0010 & 1111  ->  0010 ^ 1101  ->  1111
        for (int i = 1; i < round; i++) {
            r ^= ((n >>> (i * bits)) & mask);
        }
        return r;
    }

    /**
     * Returns a power of two size for the given target capacity.
     */
    @SuppressWarnings("all")
    private static int segmentBucketsFor(int c) {
        int n = c - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

}
