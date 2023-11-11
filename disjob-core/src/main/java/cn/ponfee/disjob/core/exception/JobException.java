/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.exception;

import cn.ponfee.disjob.common.exception.BaseException;
import cn.ponfee.disjob.common.model.CodeMsg;

/**
 * Job checked exception definition.
 *
 * @author Ponfee
 */
public class JobException extends BaseException {
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
