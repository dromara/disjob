package cn.ponfee.scheduler.common.concurrent;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Concurrent hash set based {@link ConcurrentHashMap}
 *
 * @param <E> the element type
 * @author Ponfee
 */
public class ConcurrentHashSet<E> extends AbstractSet<E> implements Set<E>, java.io.Serializable {

    private static final Object PRESENT = new Object();

    private final ConcurrentMap<E, Object> delegate;

    public ConcurrentHashSet() {
        delegate = new ConcurrentHashMap<>();
    }

    public ConcurrentHashSet(int initialCapacity) {
        delegate = new ConcurrentHashMap<>(initialCapacity);
    }

    @Override
    public Iterator<E> iterator() {
        return delegate.keySet().iterator();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return delegate.containsKey(o);
    }

    @Override
    public boolean add(E e) {
        return delegate.put(e, PRESENT) == null;
    }

    @Override
    public boolean remove(Object o) {
        return delegate.remove(o) == PRESENT;
    }

    @Override
    public void clear() {
        delegate.clear();
    }

}
