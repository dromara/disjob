/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.exception;

import cn.ponfee.disjob.common.exception.BaseRuntimeException;
import cn.ponfee.disjob.common.model.CodeMsg;

/**
 * Job unchecked exception definition.
 *
 * @author Ponfee
 */
public class JobRuntimeException extends BaseRuntimeException {
    private static final long serialVersionUID = -5627922900462363679L;

    public JobRuntimeException(int code) {
        this(code, null, null);
    }

    public JobRuntimeException(int code, String message) {
        this(code, message, null);
    }

    public JobRuntimeException(int code, Throwable cause) {
        this(code, null, cause);
    }

    public JobRuntimeException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

    public JobRuntimeException(CodeMsg cm) {
        super(cm.getCode(), cm.getMsg(), null);
    }

    public JobRuntimeException(CodeMsg cm, String message) {
        super(cm.getCode(), message, null);
    }

    public JobRuntimeException(CodeMsg cm, String message, Throwable cause) {
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
    public JobRuntimeException(int code, String message, Throwable cause,
                               boolean enableSuppression, boolean writableStackTrace) {
        super(code, message, cause, enableSuppression, writableStackTrace);
    }

}
