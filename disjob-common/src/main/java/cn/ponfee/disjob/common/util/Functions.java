/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Function utilities.
 *
 * @author Ponfee
 */
public class Functions {

    public static <T, R> Function<T, R> convert(final Consumer<T> consumer, final R result) {
        return t -> {
            consumer.accept(t);
            return result;
        };
    }

    public static <T> Predicate<T> convert(final Consumer<T> consumer, boolean result) {
        return t -> {
            consumer.accept(t);
            return result;
        };
    }

    public static <T> T doIfTrue(Supplier<T> supplier, Predicate<T> predicate, Runnable action) {
        T result = supplier.get();
        if (predicate.test(result)) {
            action.run();
        }
        return result;
    }

    public static <T> T doIfTrue(Supplier<T> supplier, T expect, Runnable action) {
        T result = supplier.get();
        if (Objects.equals(result, expect)) {
            action.run();
        }
        return result;
    }

    public static boolean doIfTrue(boolean state, Runnable action) {
        if (state) {
            action.run();
        }
        return state;
    }

}
