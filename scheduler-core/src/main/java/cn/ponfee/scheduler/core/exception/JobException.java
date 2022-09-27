package cn.ponfee.scheduler.core.exception;

import cn.ponfee.scheduler.common.base.exception.BaseCheckedException;
import cn.ponfee.scheduler.common.base.model.CodeMsg;

/**
 * Job exception definition.
 *
 * @author Ponfee
 */
public class JobException extends BaseCheckedException {

    private static final long serialVersionUID = -6568546076593428337L;

    public JobException(int code) {
        this(code, null, null);
    }

    public JobException(int code, String message) {
        this(code, message, null);
    }

    public JobException(int code, Throwable cause) {
        this(code, null, cause);
    }

    public JobException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

    public JobException(CodeMsg cm) {
        super(cm.getCode(), cm.getMsg(), null);
    }

    public JobException(CodeMsg cm, String message) {
        super(cm.getCode(), message, null);
    }

    public JobException(CodeMsg cm, String message, Throwable cause) {
        super(cm.getCode(), message, cause);
    }

    /**
     * Creates JobException
     *
     * @param code               the code
     * @param message            the message
     * @param cause              the cause
     * @param enableSuppression  the enableSuppression
     * @param writableStackTrace the writableStackTrace
     */
    public JobException(int code, String message, Throwable cause,
                        boolean enableSuppression, boolean writableStackTrace) {
        super(code, message, cause, enableSuppression, writableStackTrace);
    }

}
