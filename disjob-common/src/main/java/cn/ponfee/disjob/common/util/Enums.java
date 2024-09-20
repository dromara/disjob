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

package cn.ponfee.disjob.common.util;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.EnumUtils;

import java.util.Arrays;
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
    public static <E extends Enum<E>> ImmutableMap<String, E> toMap(Class<E> enumType) {
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
    public static <K, E extends Enum<E>> ImmutableMap<K, E> toMap(Class<E> enumType, Function<E, K> keyMapper) {
        return Arrays.stream(enumType.getEnumConstants())
            .collect(ImmutableMap.toImmutableMap(keyMapper, Function.identity()));
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
