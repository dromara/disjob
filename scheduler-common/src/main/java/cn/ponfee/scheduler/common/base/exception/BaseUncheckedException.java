package cn.ponfee.scheduler.common.base.exception;

import cn.ponfee.scheduler.common.base.model.CodeMsg;

/**
 * Base unchecked exception definition
 *
 * @author Ponfee
 */
public abstract class BaseUncheckedException extends RuntimeException {
    private static final long serialVersionUID = 5960744547215706709L;

    /**
     * Error code
     */
    private final int code;

    public BaseUncheckedException(int code) {
        this(code, null, null);
    }

    public BaseUncheckedException(CodeMsg codeMsg) {
        this(codeMsg.getCode(), codeMsg.getMsg(), null);
    }

    /**
     * @param code    error code
     * @param message error message
     */
    public BaseUncheckedException(int code, String message) {
        this(code, message, null);
    }

    public BaseUncheckedException(CodeMsg codeMsg, Throwable cause) {
        this(codeMsg.getCode(), codeMsg.getMsg(), cause);
    }

    /**
     * @param code    error code
     * @param message error message
     * @param cause   root cause
     */
    public BaseUncheckedException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * @param code               error code
     * @param message            error message
     * @param cause              root cause
     * @param enableSuppression  the enableSuppression
     * @param writableStackTrace then writableStackTrace
     */
    public BaseUncheckedException(int code,
                                  String message,
                                  Throwable cause,
                                  boolean enableSuppression,
                                  boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.code = code;
    }

    /**
     * Returns the error code
     *
     * @return int value of error code
     */
    public int getCode() {
        return code;
    }

}
