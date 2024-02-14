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

package cn.ponfee.disjob.common.tuple;

import cn.ponfee.disjob.common.collect.DelegatedIntSpliterator;
import cn.ponfee.disjob.common.collect.ImmutableArrayList;
import cn.ponfee.disjob.common.util.Comparators;
import cn.ponfee.disjob.common.util.ObjectUtils;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

/**
 * Abstract Tuple type.
 *
 * @author Ponfee
 */
public abstract class Tuple implements Comparable<Object>, Iterable<Object>, Serializable {
    private static final long serialVersionUID = -3292038317953347997L;

    /**
     * Get the object at the given index.
     *
     * @param index The index of the object to retrieve. Starts at 0.
     * @return The object or {@literal null} if out of bounds.
     */
    public abstract <T> T get(int index);

    /**
     * Set the value at the given index.
     *
     * @param value The object value.
     * @param index The index of the object to retrieve. Starts at 0.
     */
    public abstract <T> void set(T value, int index);

    /**
     * Returns a copy of this instance.
     *
     * @return a copy of this instance.
     */
    public abstract <T extends Tuple> T copy();

    /**
     * Returns int value of this tuple elements count.
     *
     * @return int value of this tuple elements count.
     */
    public abstract int length();

    /**
     * Turn this {@code Tuple} into a plain {@code Object[]}.
     * The array isn't tied to this Tuple but is a <strong>copy</strong>.
     *
     * @return A copy of the tuple as a new {@link Object Object[]}.
     */
    public final Object[] toArray() {
        int len = length();
        Object[] array = new Object[len];
        for (int i = 0; i < len; i++) {
            array[i] = get(i);
        }
        return array;
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the Tuple.
     */
    @Override
    public final String toString() {
        return join(", ", String::valueOf, "(", ")");
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public final int hashCode() {
        int len = length();
        if (len < 1) {
            return 0;
        }

        int hash = Objects.hashCode(get(0));
        for (int i = 1; i < len; i++) {
            hash = 31 * hash + Objects.hashCode(get(i));
        }
        return hash;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param obj the reference object with which to compare.
     * @return {@code true} if this object equals the other.
     */
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        Tuple other = (Tuple) obj;
        for (int i = 0, len = length(); i < len; i++) {
            if (!Objects.equals(this.get(i), other.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns tuple elements are equals array elements.
     *
     * @param elements the elements
     * @return {@code true} if elements equals.
     */
    public final boolean equals(Object... elements) {
        int len;
        if (elements == null || elements.length != (len = length())) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (!Objects.equals(get(i), elements[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public final int compareTo(Object o) {
        if (this == o) {
            return Comparators.EQ;
        }
        if (!(o instanceof Tuple)) {
            return ObjectUtils.compare(this, o);
        }

        Tuple other = (Tuple) o;
        for (int c, i = 0, n = this.length(); i < n; i++) {
            c = ObjectUtils.compare(this.get(i), other.get(i));
            if (c != Comparators.EQ) {
                return c;
            }
        }

        return Comparators.EQ;
    }

    /**
     * Turn this {@code Tuple} into a {@link List List&lt;Object&gt;}.
     * The list isn't tied to this Tuple but is a <strong>copy</strong> with limited
     * mutability ({@code add} and {@code remove} are not supported, but {@code set} is).
     *
     * @return A copy of the tuple as a new {@link List List&lt;Object&gt;}.
     */
    public List<Object> toList() {
        return ImmutableArrayList.of(toArray());
    }

    /**
     * Return an <strong>immutable</strong> {@link Iterator Iterator&lt;Object&gt;} around
     * the content of this {@code Tuple}.
     *
     * @return An unmodifiable {@link Iterator} over the elements in this Tuple.
     * @implNote As an {@link Iterator} is always tied to its {@link Iterable} source by
     * definition, the iterator cannot be mutable without the iterable also being mutable.
     */
    @Override
    public Iterator<Object> iterator() {
        // Also use: toList().iterator();
        return new TupleIterator<>();
    }

    @Override
    public Spliterator<Object> spliterator() {
        return new DelegatedIntSpliterator<>(0, length(), this::get);
    }

    /**
     * Returns string of joined the tuple elements.
     *
     * @param delimiter   the delimiter
     * @param valueMapper the valueMapper for each element to string function
     * @param prefix      the prefix
     * @param suffix      the suffix
     * @return string of joined the tuple elements
     */
    public final String join(CharSequence delimiter,
                             Function<Object, String> valueMapper,
                             CharSequence prefix,
                             CharSequence suffix) {
        StringBuilder builder = new StringBuilder(prefix);
        for (int i = 0, n = length() - 1; i <= n; i++) {
            builder.append(valueMapper.apply(get(i)));
            if (i < n) {
                builder.append(delimiter);
            }
        }
        return builder.append(suffix).toString();
    }

    /**
     * Tuple Iterator
     *
     * @param <T> element type
     */
    private class TupleIterator<T> implements Iterator<T> {
        private int position = 0;
        private final int size = length();

        @Override
        public boolean hasNext() {
            return position < size;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return get(position++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
