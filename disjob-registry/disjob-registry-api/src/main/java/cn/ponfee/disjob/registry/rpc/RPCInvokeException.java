/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry.rpc;

/**
 * RPC invoke exception
 *
 * @author Ponfee
 */
public class RPCInvokeException extends RuntimeException {
    private static final long serialVersionUID = -2137715994975702313L;

    public RPCInvokeException() {
        super();
    }

    public RPCInvokeException(String message) {
        super(message);
    }

    public RPCInvokeException(Throwable cause) {
        super(cause);
    }

    public RPCInvokeException(String message, Throwable cause) {
        super(message, cause);
    }

    protected RPCInvokeException(String message,
                                 Throwable cause,
                                 boolean enableSuppression,
                                 boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
