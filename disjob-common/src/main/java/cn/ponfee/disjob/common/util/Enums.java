/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.EnumUtils;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Enum utility
 *
 * @author Ponfee
 */
public class Enums {

    /**
     * Gets the {@code Map} of enums by name.
     *
     * @param enumType the enum type
     * @param <E>      map key mapper
     * @return the immutable map of enum to map enums, never null
     * @see EnumUtils#getEnumMap(Class)
     */
    public static <E extends Enum<E>> Map<String, E> toMap(Class<E> enumType) {
        return toMap(enumType, Enum::name);
    }

    /**
     * Returns {@code Map} of enum
     *
     * @param enumType  the enum type
     * @param keyMapper map key mapper
     * @param <K>       then map key type
     * @param <E>       the enum type
     * @return the immutable map of enum to map enums, never null
     */
    public static <K, E extends Enum<E>> Map<K, E> toMap(Class<E> enumType, Function<E, K> keyMapper) {
        ImmutableMap.Builder<K, E> result = ImmutableMap.builder();
        for (final E e: enumType.getEnumConstants()) {
            result.put(keyMapper.apply(e), e);
        }
        return result.build();
    }

    public static <E extends Enum<E>> void checkDuplicated(Class<E> enumType, Function<E, ?> mapper) {
        E[] values = enumType.getEnumConstants();
        for (int n = values.length, i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                Object v1 = mapper.apply(values[i]), v2 = mapper.apply(values[j]);
                if (Objects.equals(v1, v2)) {
                    throw new Error(enumType.getSimpleName() + " enums duplicated job error code: " + v1);
                }
            }
        }
    }

}
