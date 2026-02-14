/*
 * Copyright 2022-2026 Ponfee (http://www.ponfee.cn/)
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

package cn.ponfee.disjob.common.exception;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Referenced `io.vavr.control.Try` from `io.vavr:vavr:1.0.0`
 *
 * @param <R> the type of the value in case of success
 * @author ponfee.fu
 */
public interface Try<R> extends Serializable {

    long serialVersionUID = 1L;

    static <T extends Throwable> Try<Void> run(Throwables.ThrowingRunnable<T> runnable) {
        Objects.requireNonNull(runnable, "runnable is null");
        try {
            runnable.run();
            return new Success<>(null);
        } catch (Throwable t) {
            return new Failure<>(t);
        }
    }

    static <E, T extends Throwable> Try<Void> run(Throwables.ThrowingConsumer<E, T> consumer, E arg) {
        Objects.requireNonNull(consumer, "consumer is null");
        try {
            consumer.accept(arg);
            return new Success<>(null);
        } catch (Throwable t) {
            return new Failure<>(t);
        }
    }

    static <R, T extends Throwable> Try<R> call(Throwables.ThrowingSupplier<R, T> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        try {
            return new Success<>(supplier.get());
        } catch (Throwable t) {
            return new Failure<>(t);
        }
    }

    static <E, R, T extends Throwable> Try<R> call(Throwables.ThrowingFunction<E, R, T> function, E arg) {
        Objects.requireNonNull(function, "function is null");
        try {
            return new Success<>(function.apply(arg));
        } catch (Throwable t) {
            return new Failure<>(t);
        }
    }

    static <R> Try<R> success(R value) {
        return new Success<>(value);
    }

    static <R> Try<R> failure(Throwable exception) {
        return new Failure<>(exception);
    }

    R get();

    Throwable getCause();

    boolean isFailure();

    boolean isSuccess();

    default Try<R> orElse(Try<R> other) {
        Objects.requireNonNull(other, "other is null");
        return isSuccess() ? this : other;
    }

    default Try<R> orElseGet(Supplier<Try<R>> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return isSuccess() ? this : supplier.get();
    }

    default Try<R> orElseGet(Throwables.ThrowingSupplier<R, ?> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return isSuccess() ? this : call(supplier);
    }

    final class Success<R> implements Try<R>, Serializable {
        private static final long serialVersionUID = 1L;
        private final R value;

        private Success(R value) {
            this.value = value;
        }

        @Override
        public R get() {
            return value;
        }

        @Override
        public Throwable getCause() {
            throw new UnsupportedOperationException("getCause on Success");
        }

        @Override
        public boolean isFailure() {
            return false;
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public String toString() {
            return "Success(" + value + ")";
        }
    }

    final class Failure<R> implements Try<R>, Serializable {
        private static final long serialVersionUID = 1L;
        private final Throwable cause;

        private Failure(Throwable cause) {
            Objects.requireNonNull(cause, "cause is null");
            this.cause = Throwables.isFatal(cause) ? ExceptionUtils.rethrow(cause) : cause;
        }

        @Override
        public R get() {
            return ExceptionUtils.rethrow(cause);
        }

        @Override
        public Throwable getCause() {
            return cause;
        }

        @Override
        public boolean isFailure() {
            return true;
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public String toString() {
            return "Failure(" + cause + ")";
        }
    }

}
