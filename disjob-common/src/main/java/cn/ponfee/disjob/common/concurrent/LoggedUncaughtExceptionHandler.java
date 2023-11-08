/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logged uncaught exception handler
 *
 * @author Ponfee
 */
public final class LoggedUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final static Logger LOG = LoggerFactory.getLogger(LoggedUncaughtExceptionHandler.class);

    public static final LoggedUncaughtExceptionHandler INSTANCE = new LoggedUncaughtExceptionHandler();

    private LoggedUncaughtExceptionHandler() {
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (e instanceof java.lang.ThreadDeath) {
            LOG.warn("Uncaught exception handle, thread death: {} | {}", t.getName(), e.getMessage());
        } else if (e instanceof InterruptedException) {
            LOG.warn("Uncaught exception handle, thread interrupted: {} | {}", t.getName(), e.getMessage());
        } else {
            LOG.error("Uncaught exception handle, occur error: " + t.getName(), e);
        }
    }

}
