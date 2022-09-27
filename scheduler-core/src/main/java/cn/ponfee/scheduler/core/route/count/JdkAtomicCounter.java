package cn.ponfee.scheduler.core.route.count;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Atomic counter based jdk AtomicLong class
 *
 * @author Ponfee
 * @see java.util.concurrent.atomic.AtomicLong
 */
public class JdkAtomicCounter extends AtomicCounter {

    private final AtomicLong counter;

    public JdkAtomicCounter() {
        this(1);
    }

    public JdkAtomicCounter(long initialValue) {
        this.counter = new AtomicLong(initialValue);
    }

    @Override
    public long get() {
        return counter.get();
    }

    @Override
    public void set(long newValue) {
        counter.set(newValue);
    }

    @Override
    public long getAndAdd(long delta) {
        return counter.getAndAdd(delta);
    }
}
