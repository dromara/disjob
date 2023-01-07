/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.base.exception;

import cn.ponfee.scheduler.common.base.model.CodeMsg;

/**
 * Base checked exception definition
 *
 * @author Ponfee
 */
public abstract class BaseCheckedException extends Exception {
    private static final long serialVersionUID = -5891689205125494699L;

    /**
     * Error code
     */
    private final int code;

    public BaseCheckedException(int code) {
        this(code, null, null);
    }

    public BaseCheckedException(CodeMsg codeMsg) {
        this(codeMsg.getCode(), codeMsg.getMsg(), null);
    }

    /**
     * @param code    error code
     * @param message error message
     */
    public BaseCheckedException(int code, String message) {
        this(code, message, null);
    }

    public BaseCheckedException(CodeMsg codeMsg, Throwable cause) {
        this(codeMsg.getCode(), codeMsg.getMsg(), cause);
    }

    /**
     * @param code    error code
     * @param message error message
     * @param cause   root cause
     */
    public BaseCheckedException(int code, String message, Throwable cause) {
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
    public BaseCheckedException(int code,
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
