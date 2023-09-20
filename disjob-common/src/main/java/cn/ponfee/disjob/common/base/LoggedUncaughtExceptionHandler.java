/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logged uncaught exception handler
 *
 * @author Ponfee
 */
public class LoggedUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final static Logger LOG = LoggerFactory.getLogger(LoggedUncaughtExceptionHandler.class);
    public static final LoggedUncaughtExceptionHandler INSTANCE = new LoggedUncaughtExceptionHandler();

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        LOG.error("Logging thread uncaught exception [" + t.getName() + "]", e);
    }

}
