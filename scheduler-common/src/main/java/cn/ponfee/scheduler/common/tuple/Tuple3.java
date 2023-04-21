/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.tuple;

import java.util.Objects;

/**
 * Tuple3 consisting of three elements.
 *
 * @author Ponfee
 */
public final class Tuple3<A, B, C> extends Tuple {
    private static final long serialVersionUID = -8101132015890693468L;

    public A a;
    public B b;
    public C c;

    public Tuple3(A a, B b, C c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public static <A, B, C> Tuple3<A, B, C> of(A a, B b, C c) {
        return new Tuple3<>(a, b, c);
    }

    @Override
    public <T> T get(int index) {
        switch (index) {
            case  0: return (T) a;
            case  1: return (T) b;
            case  2: return (T) c;
            default: throw new IndexOutOfBoundsException("Index: " + index);
        }
    }

    @Override
    public <T> void set(T value, int index) {
        switch (index) {
            case  0: a = (A) value; break;
            case  1: b = (B) value; break;
            case  2: c = (C) value; break;
            default: throw new IndexOutOfBoundsException("Index: " + index);
        }
    }

    @Override
    public Object[] toArray() {
        return new Object[]{a, b, c};
    }

    @Override
    public String toString() {
        return "(" + a + ", " + b + ", " + c + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Tuple3)) {
            return false;
        }

        Tuple3<?, ?, ?> o = (Tuple3<?, ?, ?>) obj;
        return eq(o.a, o.b, o.c);
    }

    public boolean eq(Object a, Object b, Object c) {
        return Objects.equals(this.a, a)
            && Objects.equals(this.b, b)
            && Objects.equals(this.c, c);
    }

    @Override
    public int hashCode() {
        int result = a != null ? a.hashCode() : 0;
        result = HASH_FACTOR * result + (b != null ? b.hashCode() : 0);
        result = HASH_FACTOR * result + (c != null ? c.hashCode() : 0);
        return result;
    }

    @Override
    public int length() {
        return 3;
    }

    @Override
    public Tuple3<A, B, C> copy() {
        return new Tuple3<>(a, b, c);
    }

}
