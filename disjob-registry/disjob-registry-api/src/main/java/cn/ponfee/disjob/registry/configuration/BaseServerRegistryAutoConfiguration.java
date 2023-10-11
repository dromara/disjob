/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base server registry autoConfiguration
 *
 * @author Ponfee
 */
public abstract class BaseServerRegistryAutoConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(BaseServerRegistryAutoConfiguration.class);
    private static final AtomicBoolean MUTEX = new AtomicBoolean(false);

    public BaseServerRegistryAutoConfiguration() {
        if (MUTEX.compareAndSet(false, true)) {
            LOG.info("Enabled registry center '{}'", getClass());
        } else {
            throw new Error("Enable registry center '" + getClass() + "' failed, imported more than one registry center.");
        }
    }

}
