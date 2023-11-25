/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;

import java.util.function.BooleanSupplier;

/**
 * Sleep wait utility.
 *
 * @author Ponfee
 */
public class SleepWaitUtils {

    public static boolean waitUntil(int round, long[] sleepMillis, BooleanSupplier supplier) {
        return waitUntil(round, sleepMillis, true, supplier);
    }

    public static boolean waitUntil(int round, long[] sleepMillis, boolean caught, BooleanSupplier supplier) {
        int lastIndex = sleepMillis.length - 1;
        for (int i = 0; i < round; i++) {
            long sleepTime = sleepMillis[Math.min(i, lastIndex)];
            if (sleepTime > 0) {
                if (caught) {
                    ThrowingRunnable.doCaught(() -> Thread.sleep(sleepTime));
                } else {
                    ThrowingRunnable.doChecked(() -> Thread.sleep(sleepTime));
                }
            }
            if (supplier.getAsBoolean()) {
                return true;
            }
        }

        return false;
    }

}
