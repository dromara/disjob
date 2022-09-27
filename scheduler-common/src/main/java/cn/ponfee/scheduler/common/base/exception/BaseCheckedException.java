package cn.ponfee.scheduler.common.base.exception;

import cn.ponfee.scheduler.common.base.model.CodeMsg;

/**
 * Basic checked exception definition.
 *
 * @author Ponfee
 */
public abstract class BaseCheckedException extends Exception {

    private static final long serialVersionUID = -5891689205125494699L;

    /**
     * exception code.
     */
    private final int code;

    public BaseCheckedException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public BaseCheckedException(CodeMsg cm) {
        this(cm.getCode(), cm.getMsg(), null);
    }

    public BaseCheckedException(CodeMsg cm, String message) {
        this(cm.getCode(), message, null);
    }

    public BaseCheckedException(CodeMsg cm, Throwable cause) {
        this(cm.getCode(), cm.getMsg(), cause);
    }

    public BaseCheckedException(CodeMsg cm, String message, Throwable cause) {
        this(cm.getCode(), message, cause);
    }

    public BaseCheckedException(CodeMsg cm, String message, Throwable cause,
                                boolean enableSuppression, boolean writableStackTrace) {
        this(cm.getCode(), message, cause, enableSuppression, writableStackTrace);
    }

    public BaseCheckedException(CodeMsg cm, Throwable cause,
                                boolean enableSuppression, boolean writableStackTrace) {
        this(cm.getCode(), cm.getMsg(), cause, enableSuppression, writableStackTrace);
    }

    /**
     * BaseCheckedException Constructor.
     *
     * @param code               the code
     * @param message            the message
     * @param cause              the cause
     * @param enableSuppression  the enableSuppression
     * @param writableStackTrace the writableStackTrace
     */
    public BaseCheckedException(int code, String message, Throwable cause,
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
