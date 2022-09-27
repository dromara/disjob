package cn.ponfee.scheduler.common.base.exception;

import cn.ponfee.scheduler.common.base.model.CodeMsg;

/**
 * Basic unchecked exception definition.
 *
 * @author Ponfee
 */
public abstract class BaseUncheckedException extends RuntimeException {

    private static final long serialVersionUID = 5960744547215706709L;

    /**
     * exception code.
     */
    private final int code;

    public BaseUncheckedException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public BaseUncheckedException(CodeMsg cm) {
        this(cm.getCode(), cm.getMsg(), null);
    }

    public BaseUncheckedException(CodeMsg cm, String message) {
        this(cm.getCode(), message, null);
    }

    public BaseUncheckedException(CodeMsg cm, Throwable cause) {
        this(cm.getCode(), cm.getMsg(), cause);
    }

    public BaseUncheckedException(CodeMsg cm, String message, Throwable cause) {
        this(cm.getCode(), message, cause);
    }

    public BaseUncheckedException(CodeMsg cm, String message, Throwable cause,
                                  boolean enableSuppression, boolean writableStackTrace) {
        this(cm.getCode(), message, cause, enableSuppression, writableStackTrace);
    }

    public BaseUncheckedException(CodeMsg cm, Throwable cause,
                                  boolean enableSuppression, boolean writableStackTrace) {
        this(cm.getCode(), cm.getMsg(), cause, enableSuppression, writableStackTrace);
    }

    /**
     * BaseUncheckedException Constructor.
     *
     * @param code               the code
     * @param message            the message
     * @param cause              the cause
     * @param enableSuppression  the enableSuppression
     * @param writableStackTrace the writableStackTrace
     */
    public BaseUncheckedException(int code, String message, Throwable cause,
                                  boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.code = code;
    }

    /**
     * Returns error code
     *
     * @return error code
     */
    public final int getCode() {
        return code;
    }

}
