/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.concurrent;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
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
    private final Boolean daemon;
    private final Integer priority;
    private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;
    private final ThreadGroup group;

    private NamedThreadFactory(String prefix,
                               Boolean daemon,
                               Integer priority,
                               Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        if (StringUtils.isBlank(prefix)) {
            prefix = "pool-" + POOL_SEQ.getAndIncrement();
        }
        SecurityManager sm = System.getSecurityManager();
        this.prefix = prefix + "-thread-";
        this.daemon = daemon;
        this.priority = priority;
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
        this.group = sm != null ? sm.getThreadGroup() : Thread.currentThread().getThreadGroup();
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(group, Objects.requireNonNull(runnable), prefix + threadNo.getAndIncrement(), 0);
        thread.setDaemon(daemon != null ? daemon : Thread.currentThread().isDaemon());
        if (priority != null) {
            thread.setPriority(priority);
        }
        if (uncaughtExceptionHandler != null) {
            thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        }
        return thread;
    }

    public ThreadGroup getThreadGroup() {
        return group;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String prefix;
        private Boolean daemon;
        private Integer priority;
        private Thread.UncaughtExceptionHandler uncaughtExceptionHandler = LoggedUncaughtExceptionHandler.INSTANCE;

        private Builder() { }

        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder daemon(boolean daemon) {
            this.daemon = daemon;
            return this;
        }

        public Builder priority(Integer priority) {
            this.priority = priority;
            return this;
        }

        public Builder uncaughtExceptionHandler(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
            this.uncaughtExceptionHandler = uncaughtExceptionHandler;
            return this;
        }

        public NamedThreadFactory build() {
            return new NamedThreadFactory(prefix, daemon, priority, uncaughtExceptionHandler);
        }
    }

}
