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

import java.util.Comparator;

/**
 * For collection order
 *
 * @author Ponfee
 */
public final class Comparators {

    public static final int EQ =  0;
    public static final int GT =  1;
    public static final int LT = -1;

    public static <T extends Comparable<? super T>> Comparator<T> asc() {
        return Comparator.nullsFirst(Comparator.naturalOrder());
    }

    public static <T extends Comparable<? super T>> Comparator<T> desc() {
        return Comparator.nullsLast(Comparator.reverseOrder());
    }

    public static <T extends Comparable<? super T>> Comparator<T> order(boolean asc) {
        return asc ? asc() : desc();
    }

    /**
     * Compare two object numerically
     *
     * @param a the object a
     * @param b the object b
     * @return 0(a==b), 1(a>b), -1(a<b)
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static int compare(Object a, Object b) {
        if (a == b) {
            return Comparators.EQ;
        }
        if (a == null) {
            // null last
            return Comparators.GT;
        }
        if (b == null) {
            // null last
            return Comparators.LT;
        }

        if ((a instanceof Comparable) && (b instanceof Comparable)) {
            if (a.getClass().isInstance(b)) {
                return ((Comparable) a).compareTo(b);
            } else if (b.getClass().isInstance(a)) {
                return ((Comparable) b).compareTo(a);
            }
        }

        // Fields.addressOf
        int res = Integer.compare(System.identityHashCode(a.getClass()), System.identityHashCode(b.getClass()));
        return res != Comparators.EQ ? res : Integer.compare(System.identityHashCode(a), System.identityHashCode(b));
    }

}
