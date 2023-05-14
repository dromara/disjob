/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.id.snowflake;

/**
 * Clock moved back exception
 *
 * @author Ponfee
 */
public class ClockMovedBackwardsException extends RuntimeException {
    private static final long serialVersionUID = 8109219010796537426L;

    public ClockMovedBackwardsException() {
        super();
    }

    public ClockMovedBackwardsException(String message) {
        super(message);
    }

    public ClockMovedBackwardsException(Throwable cause) {
        super(cause);
    }

    public ClockMovedBackwardsException(String message, Throwable cause) {
        super(message, cause);
    }

    protected ClockMovedBackwardsException(String message,
                                           Throwable cause,
                                           boolean enableSuppression,
                                           boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
