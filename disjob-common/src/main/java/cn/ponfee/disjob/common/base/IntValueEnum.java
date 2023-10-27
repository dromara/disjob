/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.base;

import com.google.common.collect.ImmutableList;

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

    default boolean equals(Integer value) {
        return value != null && value == value();
    }

    static <T extends Enum<T> & IntValueEnum<T>> T of(Class<T> type, Integer value) {
        if (type == null) {
            throw new IllegalArgumentException("Enum int type cannot be null: " + type);
        }
        if (value == null) {
            throw new IllegalArgumentException("Enum int value cannot be null.");
        }
        for (T e : type.getEnumConstants()) {
            if (e.value() == value) {
                return e;
            }
        }
        throw new IllegalArgumentException("Invalid enum int value: " + value);
    }

    static List<IntValueDesc> values(Class<? extends IntValueEnum<?>> clazz) {
        ImmutableList.Builder<IntValueDesc> result = ImmutableList.builder();
        for (final IntValueEnum<?> e : clazz.getEnumConstants()) {
            result.add(new IntValueDesc(e.value(), e.desc()));
        }
        return result.build();
    }

}
