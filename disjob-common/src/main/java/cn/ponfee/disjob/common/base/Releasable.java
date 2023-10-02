/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.base;

import cn.ponfee.disjob.common.concurrent.Threads;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Release resources
 *
 * @author Ponfee
 */
@FunctionalInterface
public interface Releasable {

    NoArgMethodInvoker RELEASER = new NoArgMethodInvoker("close", "destroy", "release");

    /**
     * Release resources
     */
    void release();

    /**
     * Release target resources
     *
     * @param target the target object
     */
    static void release(Object target) {
        if (target == null) {
            return;
        }

        try {
            if (target instanceof AutoCloseable) {
                ((AutoCloseable) target).close();
            } else if (target instanceof Releasable) {
                Releasable releasable = (Releasable) target;
                if (!releasable.isReleased()) {
                    ((Releasable) target).release();
                }
            } else {
                RELEASER.invoke(target);
            }
        } catch (Throwable t) {
            Threads.interruptIfNecessary(t);
            ExceptionUtils.rethrow(t);
        }
    }

    /**
     * 是否已经释放，true为已经释放，false未释放
     *
     * @return {@code true}已经释放
     */
    default boolean isReleased() {
        return false;
    }

}
