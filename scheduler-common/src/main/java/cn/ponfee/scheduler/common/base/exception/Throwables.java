package cn.ponfee.scheduler.common.base.exception;

import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Throwable utilities.
 *
 * @author Ponfee
 */
public class Throwables {

    public static String getRootCauseStackTrace(Throwable throwable) {
        //return ExceptionUtils.getStackTrace(ExceptionUtils.getRootCause(throwable));
        return String.join("\n", ExceptionUtils.getRootCauseStackTrace(throwable));
    }

}
