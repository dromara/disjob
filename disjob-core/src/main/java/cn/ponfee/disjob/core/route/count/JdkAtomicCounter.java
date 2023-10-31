/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.route.count;

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
    public long addAndGet(long delta) {
        return counter.addAndGet(delta);
    }

}
