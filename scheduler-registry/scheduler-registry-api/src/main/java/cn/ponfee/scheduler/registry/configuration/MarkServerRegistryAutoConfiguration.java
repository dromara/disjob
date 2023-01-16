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
            log.info("Imported server registry: {}", getClass());
        } else {
            log.error("Conflict importing server registry: {}", getClass());
            throw new Error("Conflict importing server registry: " + getClass());
        }
    }

}
