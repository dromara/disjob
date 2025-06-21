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

import cn.ponfee.disjob.common.util.Numbers;

import java.util.Objects;
import java.util.function.Function;

/**
 * Typed dictionary key-value
 *
 * @param <K> the key type
 * @param <V> the value type
 * @author Ponfee
 */
public interface TypedDictionary<K, V> {

    V get(Object key);

    default V getOrDefault(Object key, V defaultValue) {
        V value = get(key);
        return value != null ? value : defaultValue;
    }

    default V put(K key, V value) {
        throw new UnsupportedOperationException("Cannot suppoerted put key value operation.");
    }

    default V remove(Object key) {
        throw new UnsupportedOperationException("Cannot suppoerted remove key operation.");
    }

    default boolean containsKey(Object key) {
        return get(key) != null;
    }

    default boolean containsValue(Object value) {
        throw new UnsupportedOperationException("Cannot suppoerted contains value operation.");
    }

    default V getRequired(K key) {
        return getRequired(key, Function.identity());
    }

    // --------------------------------------------------------string

    default String getRequiredString(K key) {
        return getRequired(key, Object::toString);
    }

    default String getString(K key) {
        return getString(key, null);
    }

    default String getString(K key, String defaultVal) {
        return Objects.toString(get(key), defaultVal);
    }

    default String removeString(K key) {
        return removeString(key, null);
    }

    default String removeString(K key, String defaultVal) {
        return Objects.toString(remove(key), defaultVal);
    }

    // --------------------------------------------------------boolean

    default boolean getRequiredBoolean(K key) {
        Object value = get(key);
        if (value instanceof Boolean) {
            return (boolean) value;
        }
        if (value == null) {
            throw new IllegalArgumentException("Not presented value of '" + key + "'");
        }
        switch (value.toString()) {
            case "TRUE":
            case "True":
            case "true":
                return true;
            case "FALSE":
            case "False":
            case "false":
                return false;
            default:
                throw new IllegalArgumentException("Invalid boolean value: " + value);
        }
    }

    default boolean getBoolean(K key, boolean defaultValue) {
        return Numbers.toBoolean(get(key), defaultValue);
    }

    default Boolean getBoolean(K key) {
        return Numbers.toWrapBoolean(get(key));
    }

    default boolean removeBoolean(K key, boolean defaultValue) {
        return Numbers.toBoolean(remove(key), defaultValue);
    }

    default Boolean removeBoolean(K key) {
        return Numbers.toWrapBoolean(remove(key));
    }

    // --------------------------------------------------------------int

    default int getRequiredInt(K key) {
        return getRequired(key, e -> {
            if (e instanceof Integer) {
                return (Integer) e;
            }
            return Integer.parseInt(e.toString());
        });
    }

    default int getInt(K key, int defaultValue) {
        return Numbers.toInt(get(key), defaultValue);
    }

    default Integer getInt(K key) {
        return Numbers.toWrapInt(get(key));
    }

    default int removeInt(K key, int defaultValue) {
        return Numbers.toInt(remove(key), defaultValue);
    }

    default Integer removeInt(K key) {
        return Numbers.toWrapInt(remove(key));
    }

    // --------------------------------------------------------------long

    default long getRequiredLong(K key) {
        return getRequired(key, e -> {
            if (e instanceof Integer) {
                return ((Integer) e).longValue();
            }
            if (e instanceof Long) {
                return (Long) e;
            }
            return Long.parseLong(e.toString());
        });
    }

    default long getLong(K key, long defaultValue) {
        return Numbers.toLong(get(key), defaultValue);
    }

    default Long getLong(K key) {
        return Numbers.toWrapLong(get(key));
    }

    default long removeLong(K key, long defaultValue) {
        return Numbers.toLong(remove(key), defaultValue);
    }

    default Long removeLong(K key) {
        return Numbers.toWrapLong(remove(key));
    }

    // --------------------------------------------------------------float

    default float getRequiredFloat(K key) {
        return getRequired(key, e -> {
            if (e instanceof Float) {
                return (Float) e;
            }
            return Float.parseFloat(e.toString());
        });
    }

    default float getFloat(K key, float defaultValue) {
        return Numbers.toFloat(get(key), defaultValue);
    }

    default Float getFloat(K key) {
        return Numbers.toWrapFloat(get(key));
    }

    default float removeFloat(K key, float defaultValue) {
        return Numbers.toFloat(remove(key), defaultValue);
    }

    default Float removeFloat(K key) {
        return Numbers.toWrapFloat(remove(key));
    }

    // --------------------------------------------------------------double

    default double getRequiredDouble(K key) {
        return getRequired(key, e -> {
            if (e instanceof Float) {
                return ((Float) e).doubleValue();
            }
            if (e instanceof Double) {
                return (Double) e;
            }
            return Double.parseDouble(e.toString());
        });
    }

    default double getDouble(K key, double defaultValue) {
        return Numbers.toDouble(get(key), defaultValue);

    }

    default Double getDouble(K key) {
        return Numbers.toWrapDouble(get(key));
    }

    default double removeDouble(K key, double defaultValue) {
        return Numbers.toDouble(remove(key), defaultValue);
    }

    default Double removeDouble(K key) {
        return Numbers.toWrapDouble(remove(key));
    }

    // ----------------------------------------------------other methods

    default <R> R getRequired(K key, Function<V, R> mapper) {
        V value = get(key);
        if (value == null) {
            throw new IllegalStateException("Not presented key: " + key);
        }
        R result = mapper.apply(value);
        if (result == null) {
            throw new IllegalStateException("Invalid key value: " + key + "=" + value);
        }
        return result;
    }

}
