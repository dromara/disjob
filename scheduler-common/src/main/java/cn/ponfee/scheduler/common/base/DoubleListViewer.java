/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.base;

import cn.ponfee.scheduler.common.util.Collects;
import org.springframework.util.Assert;

import java.util.*;

/**
 * Double(tow-dimensional) list viewer
 *
 * @param <E> the element type
 * @author Ponfee
 */
public class DoubleListViewer<E> implements List<E>, RandomAccess {
    private final Collection<List<E>> list;
    private final int size;

    public DoubleListViewer(Collection<List<E>> list) {
        Assert.notNull(list, "Origin list cannot be null.");
        this.list = list;
        this.size = list.stream().mapToInt(e -> e == null ? 0 : e.size()).sum();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object e : c) {
            if (!contains(e)) {
                return false;
            }
        }
        return false;
    }

    @Override
    public Object[] toArray() {
        return list.stream().filter(Objects::nonNull).flatMap(List::stream).toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return list.stream()
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .toArray(length -> (T[]) Collects.newArray(a.getClass(), length));
    }

    @Override
    public E get(int index) {
        Assert.isTrue(index >= 0, "Index must greater than zero.");
        if (index >= size()) {
            throw new IndexOutOfBoundsException("Index out of bounds: " + index);
        }
        int count = 0;
        for (List<E> sub : list) {
            if (sub == null || sub.isEmpty()) {
                continue;
            }
            if (index >= count + sub.size()) {
                count += sub.size();
                continue;
            }
            return sub.get(index - count);
        }

        // cannot happen
        throw new IllegalStateException();
    }

    @Override
    public int indexOf(Object o) {
        int index = 0;
        if (o == null) {
            for (List<E> sub : list) {
                if (sub == null || sub.isEmpty()) {
                    continue;
                }
                for (E e : sub) {
                    if (e == null) {
                        return index;
                    }
                    index++;
                }
            }
        } else {
            for (List<E> sub : list) {
                if (sub == null || sub.isEmpty()) {
                    continue;
                }
                for (E e : sub) {
                    if (o.equals(e)) {
                        return index;
                    }
                    index++;
                }
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        int index = size() - 1;
        if (o == null) {
            for (List<E> sub : list) {
                if (sub == null || sub.isEmpty()) {
                    continue;
                }
                for (int i = sub.size() - 1; i >= 0; i--) {
                    if (sub.get(i) == null) {
                        return index;
                    }
                    index--;
                }
            }
        } else {
            for (List<E> sub : list) {
                if (sub == null || sub.isEmpty()) {
                    continue;
                }
                for (int i = sub.size() - 1; i >= 0; i--) {
                    if (o.equals(sub.get(i))) {
                        return index;
                    }
                    index--;
                }
            }
        }
        return -1;
    }

    @Override
    public Iterator<E> iterator() {
        return new UnmodifiableIterator(0, size());
    }

    @Override
    public ListIterator<E> listIterator() {
        return new UnmodifiableListIterator(0, size());
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return new UnmodifiableListIterator(index, size());
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        Iterator<E> it = iterator();
        if (!it.hasNext()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (; ; ) {
            E e = it.next();
            sb.append(e == this ? "(this Collection)" : e);
            if (!it.hasNext()) {
                return sb.append(']').toString();
            }
            sb.append(',').append(' ');
        }
    }

    @Override
    public int hashCode() {
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
            if (!(Objects.equals(a.next(), b.next()))) {
                return false;
            }
        }
        return !(a.hasNext() || b.hasNext());
    }

    // ----------------------------------------------------Unsupported operations

    @Override
    public E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    // ----------------------------------------------------Static class

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
            return get(--position);
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
            return get(position++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
