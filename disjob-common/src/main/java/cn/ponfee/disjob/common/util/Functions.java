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
