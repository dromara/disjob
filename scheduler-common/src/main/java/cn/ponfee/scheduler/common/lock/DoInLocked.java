package cn.ponfee.scheduler.common.lock;

import java.util.concurrent.Callable;

/**
 * Do something in locked context.
 *
 * @author Ponfee
 */
@FunctionalInterface
public interface DoInLocked {

    /**
     * Run with in locked.
     *
     * @param caller the callback
     * @return exec callback result
     */
    <T> T apply(Callable<T> caller);
}
