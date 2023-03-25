/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.base.tuple;

import java.util.Objects;

/**
 * Tuple4 consisting of four elements.
 *
 * @author Ponfee
 */
public final class Tuple4<A, B, C, D> extends Tuple {
    private static final long serialVersionUID = -4282006520880127762L;

    public A a;
    public B b;
    public C c;
    public D d;

    public Tuple4(A a, B b, C c, D d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    public static <A, B, C, D> Tuple4<A, B, C, D> of(A a, B b, C c, D d) {
        return new Tuple4<>(a, b, c, d);
    }

    @Override
    public <T> T get(int index) {
        switch (index) {
            case  0: return (T) a;
            case  1: return (T) b;
            case  2: return (T) c;
            case  3: return (T) d;
            default: throw new IndexOutOfBoundsException("Index: " + index);
        }
    }

    @Override
    public <T> void set(T value, int index) {
        switch (index) {
            case  0: a = (A) value; break;
            case  1: b = (B) value; break;
            case  2: c = (C) value; break;
            case  3: d = (D) value; break;
            default: throw new IndexOutOfBoundsException("Index: " + index);
        }
    }

    @Override
    public Object[] toArray() {
        return new Object[]{a, b, c, d};
    }

    @Override
    public String toString() {
        return "(" + a + ", " + b + ", " + c + ", " + d + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Tuple4)) {
            return false;
        }

        Tuple4<?, ?, ?, ?> o = (Tuple4<?, ?, ?, ?>) obj;
        return eq(o.a, o.b, o.c, o.d);
    }

    public boolean eq(Object a, Object b, Object c, Object d) {
        return Objects.equals(this.a, a)
            && Objects.equals(this.b, b)
            && Objects.equals(this.c, c)
            && Objects.equals(this.d, d);
    }

    @Override
    public int hashCode() {
        int result = a != null ? a.hashCode() : 0;
        result = HASH_FACTOR * result + (b != null ? b.hashCode() : 0);
        result = HASH_FACTOR * result + (c != null ? c.hashCode() : 0);
        result = HASH_FACTOR * result + (d != null ? d.hashCode() : 0);
        return result;
    }

    @Override
    public int length() {
        return 4;
    }

    @Override
    public Tuple4<A, B, C, D> copy() {
        return new Tuple4<>(a, b, c, d);
    }

}
