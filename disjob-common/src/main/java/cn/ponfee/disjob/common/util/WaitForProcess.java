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
 * Wait for process
 *
 * @author Ponfee
 */
public class WaitForProcess {

    public static boolean process(int round, long[] sleepMillis, BooleanSupplier processor) {
        return process(round, sleepMillis, true, processor);
    }

    public static boolean process(int round, long[] sleepMillis, boolean caught, BooleanSupplier processor) {
        for (int i = 0; i < round; i++) {
            long sleepTime = get(sleepMillis, i);
            if (sleepTime > 0) {
                if (caught) {
                    ThrowingRunnable.execute(() -> Thread.sleep(sleepTime));
                } else {
                    ThrowingRunnable.run(() -> Thread.sleep(sleepTime));
                }
            }
            if (processor.getAsBoolean()) {
                return true;
            }
        }

        return false;
    }

    private static long get(long[] array, int index) {
        return index < array.length ? array[index] : array[array.length - 1];
    }

}
