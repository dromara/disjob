package cn.ponfee.scheduler.dispatch.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Representing a mark abstract class spring autoconfiguration for task dispatching.
 *
 * @author Ponfee
 */
public abstract class MarkTaskDispatchingAutoConfiguration {
    private static final AtomicBoolean MUTEX = new AtomicBoolean(false);

    protected final Logger log = LoggerFactory.getLogger(getClass());

    public MarkTaskDispatchingAutoConfiguration() {
        if (MUTEX.compareAndSet(false, true)) {
            log.info("Imported task dispatching: {}", getClass());
        } else {
            throw new Error("Conflict importing task dispatching: " + getClass());
        }
    }

}
