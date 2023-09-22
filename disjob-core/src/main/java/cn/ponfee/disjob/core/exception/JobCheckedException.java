/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.exception;

import cn.ponfee.disjob.common.exception.BaseCheckedException;
import cn.ponfee.disjob.common.model.CodeMsg;

/**
 * Job checked exception definition.
 *
 * @author Ponfee
 */
public class JobCheckedException extends BaseCheckedException {
    private static final long serialVersionUID = -6568546076593428337L;

    public JobCheckedException(int code) {
        this(code, null, null);
    }

    public JobCheckedException(int code, String message) {
        this(code, message, null);
    }

    public JobCheckedException(int code, Throwable cause) {
        this(code, null, cause);
    }

    public JobCheckedException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

    public JobCheckedException(CodeMsg cm) {
        super(cm.getCode(), cm.getMsg(), null);
    }

    public JobCheckedException(CodeMsg cm, String message) {
        super(cm.getCode(), message, null);
    }

    public JobCheckedException(CodeMsg cm, String message, Throwable cause) {
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
    public JobCheckedException(int code, String message, Throwable cause,
                               boolean enableSuppression, boolean writableStackTrace) {
        super(code, message, cause, enableSuppression, writableStackTrace);
    }

}
