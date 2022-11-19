package cn.ponfee.scheduler.common.base.exception;

import cn.ponfee.scheduler.common.util.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Throwable utilities.
 *
 * @author Ponfee
 */
public class Throwables {

    private static final Logger LOG = LoggerFactory.getLogger(Throwables.class);

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

}
