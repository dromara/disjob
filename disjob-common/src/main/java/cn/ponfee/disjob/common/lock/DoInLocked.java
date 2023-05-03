/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.lock;

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
    <T> T action(Callable<T> caller);
}
