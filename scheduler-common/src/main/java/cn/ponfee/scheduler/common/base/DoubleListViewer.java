package cn.ponfee.scheduler.common.base;

import cn.ponfee.scheduler.common.util.Collects;
import org.springframework.util.Assert;

import java.util.*;

/**
 * Double list viewer
 *
 * @param <T> the element type
 * @author Ponfee
 */
public class DoubleListViewer<T> implements List<T>, RandomAccess {
    private final Collection<List<T>> list;
    private final int size;

    public DoubleListViewer(Collection<List<T>> list) {
        Assert.isTrue(list != null, "Origin list cannot be null.");
        this.list = list;
        this.size = list.stream().filter(Objects::nonNull).mapToInt(List::size).sum();
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
    public T get(int index) {
        Assert.isTrue(index >= 0, "Index must greater than zero.");
        if (index >= size()) {
            throw new IndexOutOfBoundsException("Index out of bounds: " + index);
        }
        int count = 0;
        for (List<T> sub : list) {
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
            for (List<T> sub : list) {
                if (sub == null || sub.isEmpty()) {
                    continue;
                }
                for (int i = 0; i < sub.size(); i++) {
                    if (sub.get(i) == null) {
                        return index;
                    }
                    index++;
                }
            }
        } else {
            for (List<T> sub : list) {
                if (sub == null || sub.isEmpty()) {
                    continue;
                }
                for (int i = 0; i < sub.size(); i++) {
                    if (o.equals(sub.get(i))) {
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
            for (List<T> sub : list) {
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
            for (List<T> sub : list) {
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
    public Iterator<T> iterator() {
        return new UnmodifiableIterator(0, 0 + size());
    }

    @Override
    public ListIterator<T> listIterator() {
        return new UnmodifiableListIterator(0, size());
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return new UnmodifiableListIterator(index, size());
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    // ----------------------------------------------------Unsupported operations

    @Override
    public T set(int index, T element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(int index, T element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
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
    public boolean add(T t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    // ----------------------------------------------------Static class

    private class UnmodifiableListIterator extends UnmodifiableIterator implements ListIterator<T> {
        UnmodifiableListIterator(int position, int end) {
            super(position, end);
        }

        @Override
        public boolean hasPrevious() {
            return !isEmpty() && position > 0;
        }

        @Override
        public T previous() {
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
        public void set(T t) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(T t) {
            throw new UnsupportedOperationException();
        }
    }

    private class UnmodifiableIterator implements Iterator<T> {
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
