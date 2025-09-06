/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.common.concurrent;

import cn.ponfee.disjob.common.util.ObjectUtils;
import org.slf4j.Logger;

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

    private NamedThreadFactory(String prefix,
                               Boolean daemon,
                               Integer priority,
                               Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.prefix = (prefix == null ? "pool-" + POOL_SEQ.getAndIncrement() : prefix) + "-thread-";
        this.daemon = daemon;
        this.priority = priority;
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable, prefix + threadNo.getAndIncrement());
        thread.setDaemon(daemon != null ? daemon : Thread.currentThread().isDaemon());
        ObjectUtils.applyIfNotNull(priority, thread::setPriority);
        ObjectUtils.applyIfNotNull(uncaughtExceptionHandler, thread::setUncaughtExceptionHandler);
        return thread;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String prefix;
        private Boolean daemon;
        private Integer priority;
        private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

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

        public Builder uncaughtExceptionHandler(Thread.UncaughtExceptionHandler handler) {
            this.uncaughtExceptionHandler = handler;
            return this;
        }

        public Builder uncaughtExceptionHandler(Logger log) {
            this.uncaughtExceptionHandler = new LoggedUncaughtExceptionHandler(log);
            return this;
        }

        public NamedThreadFactory build() {
            return new NamedThreadFactory(prefix, daemon, priority, uncaughtExceptionHandler);
        }
    }

}
