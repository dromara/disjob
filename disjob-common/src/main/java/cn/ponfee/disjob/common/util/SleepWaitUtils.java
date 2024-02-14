/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
