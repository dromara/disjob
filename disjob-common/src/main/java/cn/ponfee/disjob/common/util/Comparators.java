/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

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
        return Comparator.naturalOrder();
    }

    public static <T extends Comparable<? super T>> Comparator<T> desc() {
        return Comparator.reverseOrder();
    }

    public static <T extends Comparable<? super T>> Comparator<T> order(boolean asc) {
        return asc ? asc() : desc();
    }

}
