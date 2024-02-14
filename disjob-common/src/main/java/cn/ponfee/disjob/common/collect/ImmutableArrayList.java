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

import cn.ponfee.disjob.common.base.ToJsonString;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Representing immutable List
 *
 * @param <E> the element type
 * @author Ponfee
 */
public class ImmutableArrayList<E> extends ToJsonString
    implements List<E>, RandomAccess, Cloneable, java.io.Serializable {

    private static final long serialVersionUID = 7013120001220709229L;

    private final E[] elements;

    public ImmutableArrayList() {
        this.elements = (E[]) Collects.EMPTY_OBJECT_ARRAY;
    }

    public ImmutableArrayList(Object[] elements) {
        // 为了节省时间及空间，此处认为外部环境不会修改数组，故不做拷贝操作
        //this.elements = (T[]) Arrays.copyOf(elements, elements.length);
        this.elements = (E[]) Objects.requireNonNull(elements);
    }

    /**
     * Returns an empty list
     *
     * @return empty list
     */
    public static <T> ImmutableArrayList<T> of() {
        return new ImmutableArrayList<>();
    }

    public static <T> ImmutableArrayList<T> of(T... array) {
        return new ImmutableArrayList<>(array);
    }

    public static <T> ImmutableArrayList<T> of(T[] array, T last) {
        return of(ArrayUtils.addAll(Objects.requireNonNull(array), last));
    }

    public static <T> ImmutableArrayList<T> of(List<T> list) {
        return of((T[]) (list.isEmpty() ? Collects.EMPTY_OBJECT_ARRAY : list.toArray()));
    }

    public static <T> ImmutableArrayList<T> of(List<T> list, T last) {
        return of((T[]) list.toArray(), last);
    }

    // ------------------------------------------------------------------------- methods
    protected int offset() {
        return 0;
    }

    @Override
    public int size() {
        return elements.length;
    }

    @Override
    public final boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public final Object[] toArray() {
        if (isEmpty()) {
            return elements.length == 0 ? elements : Collects.EMPTY_OBJECT_ARRAY;
        }
        return Arrays.copyOfRange(elements, offset(), offset() + size());
    }

    @Override
    public final <T> T[] toArray(T[] a) {
        if (isEmpty()) {
            if (a.length > 0) {
                a[0] = null;
            }
            return a;
        } else if (a.length < size()) {
            return (T[]) Arrays.copyOfRange(elements, offset(), offset() + size(), a.getClass());
        } else {
            System.arraycopy(elements, offset(), a, 0, size());
            if (a.length > size()) {
                a[size()] = null;
            }
            return a;
        }
    }

    @Override
    public final E get(int index) {
        if (index >= size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", size: " + size());
        }
        return elements[offset() + index];
    }

    @Override
    public final int indexOf(Object o) {
        if (o == null) {
            for (int i = 0, n = size(); i < n; i++) {
                if (elements[offset() + i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = 0, n = size(); i < n; i++) {
                if (o.equals(elements[offset() + i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public final int lastIndexOf(Object o) {
        if (o == null) {
            for (int i = size() - 1; i >= 0; i--) {
                if (elements[offset() + i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = size() - 1; i >= 0; i--) {
                if (o.equals(elements[offset() + i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public final boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    @Override
    public final boolean containsAll(Collection<?> c) {
        for (Object e : c) {
            if (!contains(e)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public final Iterator<E> iterator() {
        return new UnmodifiableIterator(offset(), offset() + size());
    }

    @Override
    public final ListIterator<E> listIterator() {
        return new UnmodifiableListIterator(offset(), offset() + size());
    }

    @Override
    public final ListIterator<E> listIterator(int index) {
        return new UnmodifiableListIterator(offset() + index, offset() + size());
    }

    @Override
    public final ImmutableArrayList<E> subList(int fromIndex, int toIndex) {
        Preconditions.checkPositionIndexes(fromIndex, toIndex, size());
        int length = toIndex - fromIndex;
        if (length == size()) {
            return this;
        } else if (length == 0) {
            return of((E[]) Collects.EMPTY_OBJECT_ARRAY);
        } else {
            return new SubList(offset() + fromIndex, offset() + toIndex);
        }
    }

    @Override
    public final Spliterator<E> spliterator() {
        return new DelegatedIntSpliterator<>(offset(), offset() + size(), i -> elements[i]);
    }

    @Override
    public final int hashCode() {
        int hashCode = 1;
        for (E e : this) {
            hashCode = 31 * hashCode + (e == null ? 0 : e.hashCode());
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof List)) {
            return false;
        }

        ListIterator<E> a = listIterator();
        ListIterator<?> b = ((List<?>) o).listIterator();
        while (a.hasNext() && b.hasNext()) {
            if (!Objects.equals(a.next(), b.next())) {
                return false;
            }
        }
        return !(a.hasNext() || b.hasNext());
    }

    @Override
    public Object clone() {
        return this;
    }

    public final E[] join(E last) {
        if (isEmpty()) {
            // t.getClass().getComponentType()
            return last == null ? (E[]) new Object[]{null} : ArrayUtils.toArray(last);
        }

        Class<? extends E[]> arrayType = (Class<? extends E[]>) elements.getClass();
        E[] array = arrayType.equals(Object[].class)
                  ? (E[]) new Object[size() + 1]
                  : (E[]) Array.newInstance(arrayType.getComponentType(), size() + 1);
        System.arraycopy(elements, offset(), array, 0, size());
        array[size()] = last;
        return array;
    }

    public final ImmutableArrayList<E> concat(E last) {
        return of(join(last));
    }

    // ---------------------------------------------------- unsupported operation
    @Override
    public final boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean addAll(int index, Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean removeIf(Predicate<? super E> filter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void replaceAll(UnaryOperator<E> operator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void sort(Comparator<? super E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final E remove(int index) {
        throw new UnsupportedOperationException();
    }

    private class UnmodifiableIterator implements Iterator<E> {
        protected int position;
        protected final int end;

        UnmodifiableIterator(int position, int end) {
            this.position = position;
            this.end = end;
        }

        @Override
        public boolean hasNext() {
            return !isEmpty() && position < end;
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return elements[position++];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private class UnmodifiableListIterator extends UnmodifiableIterator implements ListIterator<E> {

        UnmodifiableListIterator(int position, int end) {
            super(position, end);
        }

        @Override
        public boolean hasPrevious() {
            return !isEmpty() && position > 0;
        }

        @Override
        public E previous() {
            if (!hasPrevious()) {
                throw new NoSuchElementException();
            }
            return elements[--position];
        }

        @Override
        public int nextIndex() {
            return position;
        }

        @Override
        public int previousIndex() {
            return position - 1;
        }

        @Override
        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(E e) {
            throw new UnsupportedOperationException();
        }
    }

    private class SubList extends ImmutableArrayList<E> {
        private static final long serialVersionUID = 8017446305586649188L;

        final int offset;
        final int size;

        SubList(int from, int to) {
            super(elements);
            this.offset = from;
            this.size = to - from;
        }

        @Override
        protected int offset() {
            return offset;
        }

        @Override
        public int size() {
            return size;
        }
    }

}
