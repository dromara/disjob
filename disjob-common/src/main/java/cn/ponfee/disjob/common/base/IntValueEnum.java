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

package cn.ponfee.disjob.common.base;

import com.google.common.collect.ImmutableList;

import java.util.Arrays;
import java.util.List;

/**
 * Represents int value enum type structure.
 *
 * @param <T> enum type
 * @author Ponfee
 */
public interface IntValueEnum<T extends Enum<T> & IntValueEnum<T>> {

    /**
     * Returns enum int value
     *
     * @return enum int value
     */
    int value();

    /**
     * Returns enum description
     *
     * @return enum description
     */
    String desc();

    /**
     * Returns this IntValueEnum instance value is equals Integer value
     *
     * @param value the Integer value
     * @return {@code true} if equals
     */
    default boolean equalsValue(Integer value) {
        return value != null && value == value();
    }

    /**
     * Returns this IntValueEnum instance value is equals int value
     *
     * @param value the int value
     * @return {@code true} if equals
     */
    default boolean equalsValue(int value) {
        return value == value();
    }

    static <T extends Enum<T> & IntValueEnum<T>> T of(Class<T> type, int value) {
        if (type == null) {
            throw new IllegalArgumentException("Enum class cannot be null.");
        }
        for (T e : type.getEnumConstants()) {
            if (e.value() == value) {
                return e;
            }
        }
        throw new IllegalArgumentException("Invalid enum " + type + " int value: " + value);
    }

    static List<IntValueDesc> values(Class<? extends IntValueEnum<?>> clazz) {
        return Arrays.stream(clazz.getEnumConstants())
            .map(e -> IntValueDesc.of(e.value(), e.desc()))
            .collect(ImmutableList.toImmutableList());
    }

}
