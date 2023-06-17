/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.tuple;

import java.util.*;

/**
 * Tuple0 consisting of empty element.
 *
 * @author Ponfee
 */
public final class Tuple0 extends Tuple {
    private static final long serialVersionUID = -3627925720098458172L;
    private static final Tuple0 INSTANCE = new Tuple0();

    public Tuple0() {
    }

    public static Tuple0 of() {
        return INSTANCE;
    }

    @Override
    public <T> T get(int index) {
        throw new IndexOutOfBoundsException("Index: " + index);
    }

    @Override
    public <T> void set(T value, int index) {
        throw new IndexOutOfBoundsException("Index: " + index);
    }

    @Override
    public int length() {
        return 0;
    }

    @Override
    public Tuple0 copy() {
        return INSTANCE;
    }

    @Override
    public List<Object> toList() {
        return Collections.emptyList();
    }

    @Override
    public Iterator<Object> iterator() {
        return Collections.emptyIterator();
    }

    @Override
    public Spliterator<Object> spliterator() {
        return Spliterators.emptySpliterator();
    }

    private Object readResolve() {
        return INSTANCE;
    }
}
