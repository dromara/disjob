package cn.ponfee.scheduler.common.base;

/**
 * Represents an int value type structure.
 *
 * @author Ponfee
 */
public interface IntValue<T extends Enum<T> & IntValue<T>> {

    int value();

    default boolean equals(Integer value) {
        return value != null && value() == value;
    }

    default boolean equals(T other) {
        return other != null && value() == other.value();
    }

    static <T extends Enum<T> & IntValue<T>> T of(Class<T> type, Integer value) {
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
