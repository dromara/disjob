/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.dispatch.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base task dispatching autoConfiguration
 *
 * @author Ponfee
 */
public abstract class BaseTaskDispatchingAutoConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(BaseTaskDispatchingAutoConfiguration.class);
    private static final AtomicBoolean MUTEX = new AtomicBoolean(false);

    protected BaseTaskDispatchingAutoConfiguration() {
        if (MUTEX.compareAndSet(false, true)) {
            LOG.info("Enabled task dispatching '{}'", getClass());
        } else {
            throw new Error("Enable task dispatching '" + getClass() + "' failed, imported more than one task dispatching.");
        }
    }

}
