package cn.ponfee.scheduler.common.base;

import org.apache.commons.lang3.builder.CompareToBuilder;

import java.util.Arrays;

/**
 * The class use in Object array as hash map key
 * <p>Use for HashMap key
 *
 * @author Ponfee
 */
public final class ArrayHashKey implements java.io.Serializable, Comparable<ArrayHashKey> {

    private static final long serialVersionUID = -8749483734287105153L;

    private final Object[] key;

    public ArrayHashKey(Object... key) {
        this.key = key;
    }

    public static ArrayHashKey of(Object... key) {
        return new ArrayHashKey(key);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        return other instanceof ArrayHashKey && Arrays.equals(key, ((ArrayHashKey) other).key);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(key);
    }

    @Override
    public int compareTo(ArrayHashKey o) {
        return new CompareToBuilder().append(key, o.key).toComparison();
    }

}
