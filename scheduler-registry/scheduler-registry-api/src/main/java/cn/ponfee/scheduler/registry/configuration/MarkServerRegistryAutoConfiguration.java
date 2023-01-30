/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.registry.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Representing a mark abstract class spring autoconfiguration for server registry.
 *
 * @author Ponfee
 */
public abstract class MarkServerRegistryAutoConfiguration {
    private static final AtomicBoolean MUTEX = new AtomicBoolean(false);

    protected final Logger log = LoggerFactory.getLogger(getClass());

    public MarkServerRegistryAutoConfiguration() {
        if (MUTEX.compareAndSet(false, true)) {
            log.info("Enabled registry center '{}'", getClass());
        } else {
            throw new Error("Enable registry center '" + getClass() + "' failed, imported more than one registry center.");
        }
    }

}
