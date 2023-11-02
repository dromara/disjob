/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.dispatch.route.count;

/**
 * Atomic counter
 *
 * @author Ponfee
 */
public abstract class AtomicCounter {

    /**
     * Gets the current count value.
     *
     * @return current count value
     */
    public abstract long get();

    /**
     * Sets a new value.
     *
     * @param newValue newly value
     */
    public abstract void set(long newValue);

    /**
     * Add specified delta number and get the new value.
     *
     * @param delta the number of delta
     * @return newly value
     */
    public abstract long addAndGet(long delta);

    /**
     * Gets the current value and then add one.
     *
     * @return current value
     */
    public final long getAndIncrement() {
        return getAndAdd(1);
    }

    /**
     * Add one and get the new value.
     *
     * @return newly value
     */
    public final long incrementAndGet() {
        return addAndGet(1);
    }

    /**
     * Gets the current value and then add delta number.
     *
     * @return current value
     */
    public final long getAndAdd(long delta) {
        return addAndGet(delta) - delta;
    }

}
