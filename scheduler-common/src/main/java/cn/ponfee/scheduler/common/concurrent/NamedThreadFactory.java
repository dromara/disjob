package cn.ponfee.scheduler.common.concurrent;

import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of thread factory.
 *
 * @author Ponfee
 */
public class NamedThreadFactory implements ThreadFactory {

    private static final AtomicInteger POOL_SEQ = new AtomicInteger(1);

    private final AtomicInteger threadNo = new AtomicInteger(1);
    private final String prefix;
    private final boolean daemon;
    private final ThreadGroup group;

    public NamedThreadFactory() {
        this(null, Thread.currentThread().isDaemon());
    }

    public NamedThreadFactory(String prefix) {
        this(prefix, Thread.currentThread().isDaemon());
    }

    public NamedThreadFactory(String prefix, boolean daemon) {
        if (StringUtils.isBlank(prefix)) {
            prefix = "pool-" + POOL_SEQ.getAndIncrement();
        }
        SecurityManager securityManager = System.getSecurityManager();
        this.prefix = prefix + "-thread-";
        this.daemon = daemon;
        this.group = (securityManager == null)
            ? Thread.currentThread().getThreadGroup()
            : securityManager.getThreadGroup();
    }

    @Override
    public Thread newThread(Runnable runnable) {
        String name = prefix + threadNo.getAndIncrement();
        Thread thread = new Thread(group, runnable, name, 0);
        thread.setDaemon(daemon);
        if (thread.getPriority() != Thread.NORM_PRIORITY) {
            thread.setPriority(Thread.NORM_PRIORITY);
        }
        return thread;
    }

    public ThreadGroup getThreadGroup() {
        return group;
    }
}
