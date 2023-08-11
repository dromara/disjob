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

    // ------------------------------------------------------------------------caught

    public static Runnable caught(ThrowingRunnable<?> runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Throwable t) {
                LOG.error("", t);
                Threads.interruptIfNecessary(t);
            }
        };
    }

    public static <E> Consumer<E> caught(ThrowingConsumer<E, ?> consumer) {
        return arg -> {
            try {
                consumer.accept(arg);
            } catch (Throwable t) {
                LOG.error("", t);
                Threads.interruptIfNecessary(t);
            }
        };
    }

    public static <R> Supplier<R> caught(ThrowingSupplier<R, ?> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (Throwable t) {
                LOG.error("", t);
                Threads.interruptIfNecessary(t);
                return null;
            }
        };
    }

    public static <E, R> Function<E, R> caught(ThrowingFunction<E, R, ?> function) {
        return arg -> {
            try {
                return function.apply(arg);
            } catch (Throwable t) {
                LOG.error("", t);
                Threads.interruptIfNecessary(t);
                return null;
            }
        };
    }

    // -------------------------------------------------------------------------------interface definitions

    @FunctionalInterface
    public interface ThrowingRunnable<T extends Throwable> {
        void run() throws T;

        static void run(ThrowingRunnable<?> runnable) {
            try {
                runnable.run();
            } catch (Throwable t) {
                ExceptionUtils.rethrow(t);
            }
        }

        static <T extends Throwable> Runnable checked(ThrowingRunnable<T> runnable) {
            return () -> {
                try {
                    runnable.run();
                } catch (Throwable t) {
                    ExceptionUtils.rethrow(t);
                }
            };
        }

        static void ignored(ThrowingRunnable<?> runnable) {
            try {
                runnable.run();
            } catch (Throwable t) {
                Threads.interruptIfNecessary(t);
            }
        }

        static void caught(ThrowingRunnable<?> runnable) {
            caught(runnable, "");
        }

        static void caught(ThrowingRunnable<?> runnable, String message) {
            caught(runnable, () -> message);
        }

        static void caught(ThrowingRunnable<?> runnable, Supplier<String> message) {
            try {
                runnable.run();
            } catch (Throwable t) {
                LOG.error(message.get(), t);
                Threads.interruptIfNecessary(t);
            }
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

        static <R, T extends Throwable> Supplier<R> checked(ThrowingSupplier<R, T> supplier) {
            return () -> {
                try {
                    return supplier.get();
                } catch (Throwable t) {
                    return ExceptionUtils.rethrow(t);
                }
            };
        }

        static <R> R ignored(ThrowingSupplier<R, ?> supplier) {
            return ignored(supplier, null);
        }

        static <R> R ignored(ThrowingSupplier<R, ?> supplier, R defaultValue) {
            try {
                return supplier.get();
            } catch (Throwable t) {
                Threads.interruptIfNecessary(t);
                return defaultValue;
            }
        }

        static <R> R caught(ThrowingSupplier<R, ?> supplier) {
            return caught(supplier, null, () -> "");
        }

        static <R> R caught(ThrowingSupplier<R, ?> supplier, R defaultValue, Supplier<String> message) {
            try {
                return supplier.get();
            } catch (Throwable t) {
                LOG.error(message.get(), t);
                Threads.interruptIfNecessary(t);
                return defaultValue;
            }
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

        static <R, T extends Throwable> Callable<R> checked(ThrowingCallable<R, T> callable) {
            return () -> {
                try {
                    return callable.call();
                } catch (Throwable t) {
                    return ExceptionUtils.rethrow(t);
                }
            };
        }

        static <R> R ignored(ThrowingCallable<R, ?> callable) {
            return ignored(callable, null);
        }

        static <R> R ignored(ThrowingCallable<R, ?> callable, R defaultValue) {
            try {
                return callable.call();
            } catch (Throwable t) {
                Threads.interruptIfNecessary(t);
                return defaultValue;
            }
        }

        static <R> R caught(ThrowingCallable<R, ?> callable, String message) {
            return caught(callable, null, () -> message);
        }

        static <R> R caught(ThrowingCallable<R, ?> callable, R defaultValue, Supplier<String> message) {
            try {
                return callable.call();
            } catch (Throwable t) {
                LOG.error(message.get(), t);
                Threads.interruptIfNecessary(t);
                return defaultValue;
            }
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

        static <E, T extends Throwable> Consumer<E> checked(ThrowingConsumer<E, T> consumer) {
            return e -> {
                try {
                    consumer.accept(e);
                } catch (Throwable t) {
                    ExceptionUtils.rethrow(t);
                }
            };
        }

        static <E> void ignored(ThrowingConsumer<E, ?> consumer, E arg) {
            try {
                consumer.accept(arg);
            } catch (Throwable t) {
                Threads.interruptIfNecessary(t);
            }
        }

        static <E> void caught(ThrowingConsumer<E, ?> consumer, E arg) {
            caught(consumer, arg, null);
        }

        static <E> void caught(ThrowingConsumer<E, ?> consumer, E arg, Supplier<String> message) {
            try {
                consumer.accept(arg);
            } catch (Throwable t) {
                LOG.error(message.get(), t);
                Threads.interruptIfNecessary(t);
            }
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

        static <E, R, T extends Throwable> Function<E, R> checked(ThrowingFunction<E, R, T> function) {
            return e -> {
                try {
                    return function.apply(e);
                } catch (Throwable t) {
                    return ExceptionUtils.rethrow(t);
                }
            };
        }

        static <E, R> R ignored(ThrowingFunction<E, R, ?> function, E arg) {
            return ignored(function, arg, null);
        }

        static <E, R> R ignored(ThrowingFunction<E, R, ?> function, E arg, R defaultValue) {
            try {
                return function.apply(arg);
            } catch (Throwable t) {
                Threads.interruptIfNecessary(t);
                return defaultValue;
            }
        }

        static <E, R> R caught(ThrowingFunction<E, R, ?> function, E arg) {
            return caught(function, arg, null, () -> "");
        }

        static <E, R> R caught(ThrowingFunction<E, R, ?> function, E arg, R defaultValue, Supplier<String> message) {
            try {
                return function.apply(arg);
            } catch (Throwable t) {
                LOG.error(message.get(), t);
                Threads.interruptIfNecessary(t);
                return defaultValue;
            }
        }
    }

}
