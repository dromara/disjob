/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.tuple;

/**
 * Tuple2 consisting of two elements.
 *
 * @author Ponfee
 */
public final class Tuple2<A, B> extends Tuple {
    private static final long serialVersionUID = -3627925720098458172L;

    public A a;
    public B b;

    public Tuple2(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public static <A, B> Tuple2<A, B> of(A a, B b) {
        return new Tuple2<>(a, b);
    }

    @Override
    public <T> T get(int index) {
        switch (index) {
            case  0: return (T) a;
            case  1: return (T) b;
            default: throw new IndexOutOfBoundsException("Index: " + index);
        }
    }

    @Override
    public <T> void set(T value, int index) {
        switch (index) {
            case  0: a = (A) value; break;
            case  1: b = (B) value; break;
            default: throw new IndexOutOfBoundsException("Index: " + index);
        }
    }

    @Override
    public int length() {
        return 2;
    }

    @Override
    public Tuple2<A, B> copy() {
        return new Tuple2<>(a, b);
    }

    /**
     * Returns a Tuple2 Object of this instance swapped values.
     *
     * @return a Tuple2 Object of this instance swapped values
     */
    public Tuple2<B, A> swap() {
        return new Tuple2<>(b, a);
    }

}
