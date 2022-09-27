package cn.ponfee.scheduler.common.util;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.EnumUtils;

import java.util.Map;
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
        ImmutableMap.Builder<K, E> mapping = ImmutableMap.builder();
        for (final E e: enumType.getEnumConstants()) {
            mapping.put(keyMapper.apply(e), e);
        }
        return mapping.build();
    }
}
