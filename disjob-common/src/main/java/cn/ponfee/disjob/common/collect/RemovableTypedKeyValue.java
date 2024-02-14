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

/**
 * Removable typed dictionary key-value
 *
 * @param <K> the key type
 * @param <V> the value type
 * @author Ponfee
 */
public interface RemovableTypedKeyValue<K, V> extends TypedKeyValue<K, V> {

    V removeKey(K key);

    default String removeString(K key) {
        return removeString(key, null);
    }

    default String removeString(K key, String defaultVal) {
        return Objects.toString(removeKey(key), defaultVal);
    }

    default boolean removeBoolean(K key, boolean defaultValue) {
        return Numbers.toBoolean(removeKey(key), defaultValue);
    }

    default Boolean removeBoolean(K key) {
        return Numbers.toWrapBoolean(removeKey(key));
    }

    default int removeInt(K key, int defaultValue) {
        return Numbers.toInt(removeKey(key), defaultValue);
    }

    default Integer removeInt(K key) {
        return Numbers.toWrapInt(removeKey(key));
    }

    default long removeLong(K key, long defaultValue) {
        return Numbers.toLong(removeKey(key), defaultValue);
    }

    default Long removeLong(K key) {
        return Numbers.toWrapLong(removeKey(key));
    }

    default float removeFloat(K key, float defaultValue) {
        return Numbers.toFloat(removeKey(key), defaultValue);
    }

    default Float removeFloat(K key) {
        return Numbers.toWrapFloat(removeKey(key));
    }

    default double removeDouble(K key, double defaultValue) {
        return Numbers.toDouble(removeKey(key), defaultValue);
    }

    default Double removeDouble(K key) {
        return Numbers.toWrapDouble(removeKey(key));
    }

}
