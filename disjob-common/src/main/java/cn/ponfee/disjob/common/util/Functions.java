/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Function utilities.
 *
 * @author Ponfee
 */
public class Functions {

    public static <T, R> Function<T, R> convert(Consumer<T> consumer, final R result) {
        return t -> {
            consumer.accept(t);
            return result;
        };
    }

}
