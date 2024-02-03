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
                return "error[" + message + "]";
            }
        }

        return "error[" + ClassUtils.getName(throwable.getClass()) + "]";
    }

    // -------------------------------------------------------------------------------interface definitions

    @FunctionalInterface
    public interface ThrowingRunnable<T extends Throwable> {
        void run() throws T;

        default <R> ThrowingSupplier<R, T> toSupplier(R result) {
            return () -> {
                run();
                return result;
            };
        }

        default <R> ThrowingCallable<R, T> toCallable(R result) {
            return () -> {
                run();
                return result;
            };
        }

        static void doChecked(ThrowingRunnable<?> runnable) {
            try {
                runnable.run();
            } catch (Throwable t) {
                ExceptionUtils.rethrow(t);
            }
        }

        static void doCaught(ThrowingRunnable<?> runnable) {
            doCaught(runnable, EMPTY_MESSAGE);
        }

        static void doCaught(ThrowingRunnable<?> runnable, Supplier<String> message) {
            try {
                runnable.run();
            } catch (Throwable t) {
                LOG.error(message.get(), t);
                Threads.interruptIfNecessary(t);
            }
        }

        static Runnable toChecked(ThrowingRunnable<?> runnable) {
            return () -> {
                try {
                    runnable.run();
                } catch (Throwable t) {
                    ExceptionUtils.rethrow(t);
                }
            };
        }

        static Runnable toCaught(ThrowingRunnable<?> runnable) {
            return toCaught(runnable, EMPTY_MESSAGE);
        }

        static Runnable toCaught(ThrowingRunnable<?> runnable, Supplier<String> message) {
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

        default ThrowingRunnable<Throwable> toRunnable() {
            return this::get;
        }

        static <R> R doChecked(ThrowingSupplier<R, ?> supplier) {
            try {
                return supplier.get();
            } catch (Throwable t) {
                return ExceptionUtils.rethrow(t);
            }
        }

        static <R> R doCaught(ThrowingSupplier<R, ?> supplier) {
            return doCaught(supplier, null, EMPTY_MESSAGE);
        }

        static <R> R doCaught(ThrowingSupplier<R, ?> supplier, R defaultValue, Supplier<String> message) {
            try {
                return supplier.get();
            } catch (Throwable t) {
                LOG.error(message.get(), t);
                Threads.interruptIfNecessary(t);
                return defaultValue;
            }
        }

        static <R> Supplier<R> toChecked(ThrowingSupplier<R, ?> supplier) {
            return () -> {
                try {
                    return supplier.get();
                } catch (Throwable t) {
                    return ExceptionUtils.rethrow(t);
                }
            };
        }

        static <R> Supplier<R> toCaught(ThrowingSupplier<R, ?> supplier) {
            return toCaught(supplier, null, EMPTY_MESSAGE);
        }

        static <R> Supplier<R> toCaught(ThrowingSupplier<R, ?> supplier, R defaultValue, Supplier<String> message) {
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

        default ThrowingRunnable<T> toRunnable() {
            return this::call;
        }

        static <R> R doChecked(ThrowingCallable<R, ?> callable) {
            try {
                return callable.call();
            } catch (Throwable t) {
                return ExceptionUtils.rethrow(t);
            }
        }

        static <R> R doCaught(ThrowingCallable<R, ?> callable) {
            return doCaught(callable, null, EMPTY_MESSAGE);
        }

        static <R> R doCaught(ThrowingCallable<R, ?> callable, R defaultValue, Supplier<String> message) {
            try {
                return callable.call();
            } catch (Throwable t) {
                LOG.error(message.get(), t);
                Threads.interruptIfNecessary(t);
                return defaultValue;
            }
        }

        static <R> Callable<R> toChecked(ThrowingCallable<R, ?> callable) {
            return () -> {
                try {
                    return callable.call();
                } catch (Throwable t) {
                    return ExceptionUtils.rethrow(t);
                }
            };
        }

        static <R> Callable<R> toCaught(ThrowingCallable<R, ?> supplier) {
            return toCaught(supplier, null, EMPTY_MESSAGE);
        }

        static <R> Callable<R> toCaught(ThrowingCallable<R, ?> supplier, R defaultValue, Supplier<String> message) {
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

        default <R> ThrowingFunction<E, R, T> toFunction(R result) {
            return x -> {
                accept(x);
                return result;
            };
        }

        static <E> void doChecked(ThrowingConsumer<E, ?> consumer, E arg) {
            try {
                consumer.accept(arg);
            } catch (Throwable t) {
                ExceptionUtils.rethrow(t);
            }
        }

        static <E> void doCaught(ThrowingConsumer<E, ?> consumer, E arg) {
            doCaught(consumer, arg, EMPTY_MESSAGE);
        }

        static <E> void doCaught(ThrowingConsumer<E, ?> consumer, E arg, Supplier<String> message) {
            try {
                consumer.accept(arg);
            } catch (Throwable t) {
                LOG.error(message.get(), t);
                Threads.interruptIfNecessary(t);
            }
        }

        static <E> Consumer<E> toChecked(ThrowingConsumer<E, ?> consumer) {
            return e -> {
                try {
                    consumer.accept(e);
                } catch (Throwable t) {
                    ExceptionUtils.rethrow(t);
                }
            };
        }

        static <E> Consumer<E> toCaught(ThrowingConsumer<E, ?> consumer) {
            return toCaught(consumer, EMPTY_MESSAGE);
        }

        static <E> Consumer<E> toCaught(ThrowingConsumer<E, ?> consumer, Supplier<String> message) {
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

        default ThrowingConsumer<E, T> toConsumer() {
            return this::apply;
        }

        static <E, R> R doChecked(ThrowingFunction<E, R, ?> function, E arg) {
            try {
                return function.apply(arg);
            } catch (Throwable t) {
                return ExceptionUtils.rethrow(t);
            }
        }

        static <E, R> R doCaught(ThrowingFunction<E, R, ?> function, E arg) {
            return doCaught(function, arg, null, EMPTY_MESSAGE);
        }

        static <E, R> R doCaught(ThrowingFunction<E, R, ?> function, E arg, R defaultValue, Supplier<String> message) {
            try {
                return function.apply(arg);
            } catch (Throwable t) {
                LOG.error(message.get(), t);
                Threads.interruptIfNecessary(t);
                return defaultValue;
            }
        }

        static <E, R> Function<E, R> toChecked(ThrowingFunction<E, R, ?> function) {
            return e -> {
                try {
                    return function.apply(e);
                } catch (Throwable t) {
                    return ExceptionUtils.rethrow(t);
                }
            };
        }

        static <E, R> Function<E, R> toCaught(ThrowingFunction<E, R, ?> function) {
            return toCaught(function, null, EMPTY_MESSAGE);
        }

        static <E, R> Function<E, R> toCaught(ThrowingFunction<E, R, ?> function, R defaultValue, Supplier<String> message) {
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
