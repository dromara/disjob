/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.base.exception;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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

    // -----------------------------------------------------------------exception message

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

    // -----------------------------------------------------------------caught throwable and log error message

    public static void caught(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
            if (t instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static <R> R caught(Supplier<R> supplier) {
        try {
            return supplier.get();
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
            if (t instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    public static <E> void caught(Consumer<E> consumer, E arg) {
        try {
            consumer.accept(arg);
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
            if (t instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static <E, R> R caught(Function<E, R> function, E arg) {
        return caught(function, arg, null);
    }

    public static <E, R> R caught(Function<E, R> function, E arg, R defaultValue) {
        try {
            return function.apply(arg);
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
            if (t instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return defaultValue;
        }
    }

}
