package cn.ponfee.scheduler.common.base.tuple;

import java.util.Objects;

/**
 * Tuple1 consisting of one element.
 *
 * @author Ponfee
 */
public final class Tuple1<A> extends Tuple {
    private static final long serialVersionUID = -3627925720098458172L;

    public A a;

    public Tuple1(A a) {
        this.a = a;
    }

    public static <A> Tuple1<A> of(A a) {
        return new Tuple1<>(a);
    }

    @Override
    public <T> T get(int index) {
        if (index == 0) {
            return (T) a;
        } else {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
    }

    @Override
    public <T> void set(T value, int index) {
        if (index == 0) {
            a = (A) value;
        } else {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
    }

    @Override
    public Object[] toArray() {
        return new Object[]{a};
    }

    @Override
    public String toString() {
        return "(" + a + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return (obj instanceof Tuple1) && Objects.equals(a, ((Tuple1<?>) obj).a);
    }

    public boolean eq(Object a) {
        return Objects.equals(this.a, a);
    }

    @Override
    public int hashCode() {
        return a != null ? a.hashCode() : 0;
    }

    @Override
    public int length() {
        return 1;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Tuple1<A> copy() {
        return new Tuple1<>(a);
    }

}
