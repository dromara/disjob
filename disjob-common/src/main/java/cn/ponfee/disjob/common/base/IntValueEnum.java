/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.base;

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
        if (type == null || !type.isEnum()) {
            throw new IllegalArgumentException("Not enum type: " + type);
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null.");
        }
        for (T e : type.getEnumConstants()) {
            if (e.value() == value) {
                return e;
            }
        }
        throw new IllegalArgumentException("Invalid value: " + value);
    }

}
