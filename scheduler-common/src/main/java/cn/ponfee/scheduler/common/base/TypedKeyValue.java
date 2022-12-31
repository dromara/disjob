package cn.ponfee.scheduler.common.base;

import cn.ponfee.scheduler.common.util.Numbers;
import cn.ponfee.scheduler.common.util.ObjectUtils;

import java.util.Objects;

/**
 * Get the value with typed for dictionary key-value
 *
 * @author Ponfee
 * @param <K> the key type
 * @param <V> the value type
 */
public interface TypedKeyValue<K, V> {

    V getValue(K key);

    V removeKey(K key);

    default boolean hasKey(K key) {
        return !ObjectUtils.isEmpty(getValue(key));
    }

    // --------------------------------------------------------object

    default V getRequired(K key) {
        V value = getValue(key);
        assertPresented(key, value);
        return value;
    }

    default V get(K key, V defaultVal) {
        V value = getValue(key);
        return value == null ? defaultVal : value;
    }

    // --------------------------------------------------------string
    default String getRequiredString(K key) {
        V value = getValue(key);
        assertPresented(key, value);
        return value.toString();
    }

    default String getString(K key) {
        return getString(key, null);
    }

    default String getString(K key, String defaultVal) {
        return Objects.toString(getValue(key), defaultVal);
    }

    default String removeString(K key) {
        return removeString(key, null);
    }

    default String removeString(K key, String defaultVal) {
        return Objects.toString(removeKey(key), defaultVal);
    }

    // --------------------------------------------------------boolean
    default boolean getRequiredBoolean(K key) {
        Object value = getValue(key);
        if (value instanceof Boolean) {
            return (boolean) value;
        }
        assertPresented(key, value);
        switch (value.toString()) {
            case "TRUE" : case "True" : case "true" : return true;
            case "FALSE": case "False": case "false": return false;
            default: throw new IllegalArgumentException("Invalid boolean value: " + value);
        }
    }

    default boolean getBoolean(K key, boolean defaultValue) {
        return Numbers.toBoolean(getValue(key), defaultValue);
    }

    default Boolean getBoolean(K key) {
        return Numbers.toWrapBoolean(getValue(key));
    }

    default boolean removeBoolean(K key, boolean defaultValue) {
        return Numbers.toBoolean(removeKey(key), defaultValue);
    }

    default Boolean removeBoolean(K key) {
        return Numbers.toWrapBoolean(removeKey(key));
    }

    // --------------------------------------------------------------int
    default int getRequiredInt(K key) {
        Integer value = Numbers.toWrapInt(getValue(key));
        assertPresented(key, value);
        return value;
    }

    default int getInt(K key, int defaultValue) {
        return Numbers.toInt(getValue(key), defaultValue);
    }

    default Integer getInt(K key) {
        return Numbers.toWrapInt(getValue(key));
    }

    default int removeInt(K key, int defaultValue) {
        return Numbers.toInt(removeKey(key), defaultValue);
    }

    default Integer removeInt(K key) {
        return Numbers.toWrapInt(removeKey(key));
    }

    // --------------------------------------------------------------long
    default long getRequiredLong(K key) {
        Long value = Numbers.toWrapLong(getValue(key));
        assertPresented(key, value);
        return value;
    }

    default long getLong(K key, long defaultValue) {
        return Numbers.toLong(getValue(key), defaultValue);
    }

    default Long getLong(K key) {
        return Numbers.toWrapLong(getValue(key));
    }

    default long removeLong(K key, long defaultValue) {
        return Numbers.toLong(removeKey(key), defaultValue);
    }

    default Long removeLong(K key) {
        return Numbers.toWrapLong(removeKey(key));
    }

    // --------------------------------------------------------------float
    default float getRequiredFloat(K key) {
        Float value = Numbers.toWrapFloat(getValue(key));
        assertPresented(key, value);
        return value;
    }

    default float getFloat(K key, float defaultValue) {
        return Numbers.toFloat(getValue(key), defaultValue);
    }

    default Float getFloat(K key) {
        return Numbers.toWrapFloat(getValue(key));
    }

    default float removeFloat(K key, float defaultValue) {
        return Numbers.toFloat(removeKey(key), defaultValue);
    }

    default Float removeFloat(K key) {
        return Numbers.toWrapFloat(removeKey(key));
    }

    // --------------------------------------------------------------double
    default double getRequiredDouble(K key) {
        Double value = Numbers.toWrapDouble(getValue(key));
        assertPresented(key, value);
        return value;
    }

    default double getDouble(K key, double defaultValue) {
        return Numbers.toDouble(getValue(key), defaultValue);

    }

    default Double getDouble(K key) {
        return Numbers.toWrapDouble(getValue(key));
    }

    default double removeDouble(K key, double defaultValue) {
        return Numbers.toDouble(removeKey(key), defaultValue);
    }

    default Double removeDouble(K key) {
        return Numbers.toWrapDouble(removeKey(key));
    }

    // ---------------------------------------------------- static methods
    static void assertPresented(Object key, Object value) {
        if (ObjectUtils.isEmpty(value)) {
            throw new IllegalArgumentException("Not presented value of '" + key + "'");
        }
    }
}
