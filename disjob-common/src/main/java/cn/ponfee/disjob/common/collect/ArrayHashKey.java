/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.collect;

import org.apache.commons.lang3.builder.CompareToBuilder;

import java.util.Arrays;

/**
 * The class use in Object array as hash map key
 * <p>Use for HashMap key
 *
 * @author Ponfee
 */
public final class ArrayHashKey implements Comparable<ArrayHashKey> {

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
        return (other instanceof ArrayHashKey)
            && Arrays.equals(key, ((ArrayHashKey) other).key);
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
