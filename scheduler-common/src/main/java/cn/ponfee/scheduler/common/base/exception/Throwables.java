package cn.ponfee.scheduler.common.base.exception;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Throwable utilities.
 *
 * @author Ponfee
 */
public class Throwables {

    private static final Logger LOG = LoggerFactory.getLogger(Throwables.class);

    public static String getRootCauseStackTrace(Throwable throwable) {
        //return ExceptionUtils.getStackTrace(ExceptionUtils.getRootCause(throwable));
        return String.join("\n", ExceptionUtils.getRootCauseStackTrace(throwable));
    }

    public static void cached(Runnable runnable) {
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
