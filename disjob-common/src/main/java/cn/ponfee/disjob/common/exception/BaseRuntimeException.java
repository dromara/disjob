/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.exception;

import cn.ponfee.disjob.common.model.CodeMsg;

/**
 * Base unchecked(runtime) exception definition(运行时异常)
 *
 * @author Ponfee
 */
public abstract class BaseRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 5960744547215706709L;

    /**
     * Error code
     */
    private final int code;

    public BaseRuntimeException(int code) {
        this(code, null, null);
    }

    public BaseRuntimeException(CodeMsg codeMsg) {
        this(codeMsg.getCode(), codeMsg.getMsg(), null);
    }

    /**
     * @param code    error code
     * @param message error message
     */
    public BaseRuntimeException(int code, String message) {
        this(code, message, null);
    }

    public BaseRuntimeException(CodeMsg codeMsg, Throwable cause) {
        this(codeMsg.getCode(), codeMsg.getMsg(), cause);
    }

    /**
     * @param code    error code
     * @param message error message
     * @param cause   root cause
     */
    public BaseRuntimeException(int code, String message, Throwable cause) {
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
    public BaseRuntimeException(int code,
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
