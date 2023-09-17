/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.exception;

import cn.ponfee.disjob.common.concurrent.Threads;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Throwable utilities.
 *
 * @author Ponfee
 */
public final class Throwables {

    private static final Logger LOG = LoggerFactory.getLogger(Throwables.class);

    private static final Supplier<String> EMPTY_MESSAGE = () -> "";

    /**
     * Gets the root cause throwable stack trace
     *
     * @param throwable the throwable
     * @return a string of throwable stack trace information
     */
    public static String getRootCauseStackTrace(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        while (throwable.getCause() != null) {
            throwable = throwable.getCause();
        }
        return ExceptionUtils.getStackTrace(throwable);
    }

    public static String getRootCauseMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        List<Throwable> list = ExceptionUtils.getThrowableList(throwable);
        for (int i = list.size() - 1; i >= 0; i--) {
            String message = list.get(i).getMessage();
            if (StringUtils.isNotBlank(message)) {
                return "error: " + message;
            }
        }

        return "error: <" + ClassUtils.getName(throwable.getClass()) + ">";
    }

    // -------------------------------------------------------------------------------interface definitions

    @FunctionalInterface
    public interface ThrowingRunnable<T extends Throwable> {
        void run() throws T;

        default <E> ThrowingSupplier<E, Throwable> toSupplier(E result) {
            return () -> {
                run();
                return result;
            };
        }

        default <E> ThrowingCallable<E, Throwable> toCallable(E result) {
            return () -> {
                run();
                return result;
            };
        }

        static void run(ThrowingRunnable<?> runnable) {
            try {
                runnable.run();
            } catch (Throwable t) {
                ExceptionUtils.rethrow(t);
            }
        }

        static void execute(ThrowingRunnable<?> runnable) {
            execute(runnable, EMPTY_MESSAGE);
        }

        static void execute(ThrowingRunnable<?> runnable, Supplier<String> message) {
            try {
                runnable.run();
            } catch (Throwable t) {
                LOG.error(message.get(), t);
                Threads.interruptIfNecessary(t);
            }
        }

        static Runnable checked(ThrowingRunnable<?> runnable) {
            return () -> {
                try {
                    runnable.run();
                } catch (Throwable t) {
                    ExceptionUtils.rethrow(t);
                }
            };
        }

        static Runnable caught(ThrowingRunnable<?> runnable) {
            return caught(runnable, EMPTY_MESSAGE);
        }

        static Runnable caught(ThrowingRunnable<?> runnable, Supplier<String> message) {
            return () -> {
                try {
                    runnable.run();
                } catch (Throwable t) {
                    LOG.error(message.get(), t);
                    Threads.interruptIfNecessary(t);
                }
            };
        }
    }

    /**
     * Lambda function checked exception
     *
     * @param <R> the type of results supplied by this supplier
     * @param <T> the type of the call get method possible occur exception
     */
    @FunctionalInterface
    public interface ThrowingSupplier<R, T extends Throwable> {
        R get() throws T;

        static <R> R get(ThrowingSupplier<R, ?> supplier) {
            try {
                return supplier.get();
            } catch (Throwable t) {
                return ExceptionUtils.rethrow(t);
            }
        }

        static <R> R execute(ThrowingSupplier<R, ?> supplier) {
            return execute(supplier, null, EMPTY_MESSAGE);
        }

        static <R> R execute(ThrowingSupplier<R, ?> supplier, R defaultValue, Supplier<String> message) {
            try {
                return supplier.get();
            } catch (Throwable t) {
                LOG.error(message.get(), t);
                Threads.interruptIfNecessary(t);
                return defaultValue;
            }
        }

        static <R> Supplier<R> checked(ThrowingSupplier<R, ?> supplier) {
            return () -> {
                try {
                    return supplier.get();
                } catch (Throwable t) {
                    return ExceptionUtils.rethrow(t);
                }
            };
        }

        static <R> Supplier<R> caught(ThrowingSupplier<R, ?> supplier) {
            return caught(supplier, null, EMPTY_MESSAGE);
        }

        static <R> Supplier<R> caught(ThrowingSupplier<R, ?> supplier, R defaultValue, Supplier<String> message) {
            return () -> {
                try {
                    return supplier.get();
                } catch (Throwable t) {
                    LOG.error(message.get(), t);
                    Threads.interruptIfNecessary(t);
                    return defaultValue;
                }
            };
        }
    }

    /**
     * Lambda function checked exception
     *
     * @param <R> the result type of method {@code call}
     * @param <T> the type of the call "call" method possible occur exception
     */
    @FunctionalInterface
    public interface ThrowingCallable<R, T extends Throwable> {
        R call() throws T;

        static <R> R call(ThrowingCallable<R, ?> callable) {
            try {
                return callable.call();
            } catch (Throwable t) {
                return ExceptionUtils.rethrow(t);
            }
        }

        static <R> R execute(ThrowingCallable<R, ?> callable) {
            return execute(callable, null, EMPTY_MESSAGE);
        }

        static <R> R execute(ThrowingCallable<R, ?> callable, R defaultValue, Supplier<String> message) {
            try {
                return callable.call();
            } catch (Throwable t) {
                LOG.error(message.get(), t);
                Threads.interruptIfNecessary(t);
                return defaultValue;
            }
        }

        static <R> Callable<R> checked(ThrowingCallable<R, ?> callable) {
            return () -> {
                try {
                    return callable.call();
                } catch (Throwable t) {
                    return ExceptionUtils.rethrow(t);
                }
            };
        }

        static <R> Callable<R> caught(ThrowingCallable<R, ?> supplier) {
            return caught(supplier, null, EMPTY_MESSAGE);
        }

        static <R> Callable<R> caught(ThrowingCallable<R, ?> supplier, R defaultValue, Supplier<String> message) {
            return () -> {
                try {
                    return supplier.call();
                } catch (Throwable t) {
                    LOG.error(message.get(), t);
                    Threads.interruptIfNecessary(t);
                    return defaultValue;
                }
            };
        }
    }

    /**
     * Lambda function checked exception
     *
     * @param <E> the type of the input to the operation
     * @param <T> the type of the call accept method possible occur exception
     */
    @FunctionalInterface
    public interface ThrowingConsumer<E, T extends Throwable> {
        void accept(E e) throws T;

        static <E> void accept(ThrowingConsumer<E, ?> consumer, E arg) {
            try {
                consumer.accept(arg);
            } catch (Throwable t) {
                ExceptionUtils.rethrow(t);
            }
        }

        static <E> void execute(ThrowingConsumer<E, ?> consumer, E arg) {
            execute(consumer, arg, EMPTY_MESSAGE);
        }

        static <E> void execute(ThrowingConsumer<E, ?> consumer, E arg, Supplier<String> message) {
            try {
                consumer.accept(arg);
            } catch (Throwable t) {
                LOG.error(message.get(), t);
                Threads.interruptIfNecessary(t);
            }
        }

        static <E> Consumer<E> checked(ThrowingConsumer<E, ?> consumer) {
            return e -> {
                try {
                    consumer.accept(e);
                } catch (Throwable t) {
                    ExceptionUtils.rethrow(t);
                }
            };
        }

        static <E> Consumer<E> caught(ThrowingConsumer<E, ?> consumer) {
            return caught(consumer, EMPTY_MESSAGE);
        }

        static <E> Consumer<E> caught(ThrowingConsumer<E, ?> consumer, Supplier<String> message) {
            return arg -> {
                try {
                    consumer.accept(arg);
                } catch (Throwable t) {
                    LOG.error(message.get(), t);
                    Threads.interruptIfNecessary(t);
                }
            };
        }
    }

    /**
     * Lambda function checked exception
     *
     * @param <E> the type of the input to the function
     * @param <R> the type of the result of the function
     * @param <T> the type of the call apply method possible occur exception
     */
    @FunctionalInterface
    public interface ThrowingFunction<E, R, T extends Throwable> {
        R apply(E e) throws T;

        static <E, R> R apply(ThrowingFunction<E, R, ?> function, E arg) {
            try {
                return function.apply(arg);
            } catch (Throwable t) {
                return ExceptionUtils.rethrow(t);
            }
        }

        static <E, R> R execute(ThrowingFunction<E, R, ?> function, E arg) {
            return execute(function, arg, null, EMPTY_MESSAGE);
        }

        static <E, R> R execute(ThrowingFunction<E, R, ?> function, E arg, R defaultValue, Supplier<String> message) {
            try {
                return function.apply(arg);
            } catch (Throwable t) {
                LOG.error(message.get(), t);
                Threads.interruptIfNecessary(t);
                return defaultValue;
            }
        }

        static <E, R> Function<E, R> checked(ThrowingFunction<E, R, ?> function) {
            return e -> {
                try {
                    return function.apply(e);
                } catch (Throwable t) {
                    return ExceptionUtils.rethrow(t);
                }
            };
        }

        static <E, R> Function<E, R> caught(ThrowingFunction<E, R, ?> function) {
            return caught(function, null, EMPTY_MESSAGE);
        }

        static <E, R> Function<E, R> caught(ThrowingFunction<E, R, ?> function, R defaultValue, Supplier<String> message) {
            return arg -> {
                try {
                    return function.apply(arg);
                } catch (Throwable t) {
                    LOG.error(message.get(), t);
                    Threads.interruptIfNecessary(t);
                    return defaultValue;
                }
            };
        }
    }

}
