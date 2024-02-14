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
