/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

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
