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

package cn.ponfee.disjob.core.util;

import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.concurrent.Threads;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingSupplier;
import cn.ponfee.disjob.common.spring.SpringContextHolder;
import cn.ponfee.disjob.common.util.NetUtils;
import cn.ponfee.disjob.core.base.JobConstants;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

import static cn.ponfee.disjob.core.base.JobConstants.DISJOB_BOUND_SERVER_HOST;

/**
 * Disjob utility
 *
 * @author Ponfee
 */
public class DisjobUtils {

    private static final Logger LOG = LoggerFactory.getLogger(DisjobUtils.class);

    /**
     * Instance lock pool
     */
    public static final Interner<Long> INSTANCE_LOCK_POOL = Interners.newWeakInterner();

    public static String getLocalHost() {
        return getLocalHost(SpringContextHolder.getProperty(DISJOB_BOUND_SERVER_HOST));
    }

    public static String getLocalHost(String specifiedHost) {
        String host = specifiedHost;
        if (isValidHost(host, "specified")) {
            return host;
        }

        host = System.getProperty(DISJOB_BOUND_SERVER_HOST);
        if (isValidHost(host, "System#getProperty")) {
            return host;
        }

        host = System.getenv(DISJOB_BOUND_SERVER_HOST);
        if (isValidHost(host, "System#getenv")) {
            return host;
        }

        host = NetUtils.getLocalHost();
        if (isValidHost(host, "NetUtils#getLocalHost")) {
            return host;
        }

        throw new Error("Not found available server host.");
    }

    public static <R> R doInSynchronized(Long lock, ThrowingSupplier<R, ?> action) throws Throwable {
        synchronized (INSTANCE_LOCK_POOL.intern(lock)) {
            return action.get();
        }
    }

    public static void doInSynchronized(Long lock, ThrowingRunnable<?> action, Supplier<String> message) {
        Throwable t = null;
        try {
            doInSynchronized(lock, action);
        } catch (Throwable e) {
            t = e;
            LOG.error(message.get(), t);
        } finally {
            if (isCurrentThreadInterrupted(t)) {
                boolean interrupted = Thread.currentThread().isInterrupted();
                LOG.info("Do synchronized retry interrupted {}, {}", interrupted, message.get());
                ThreadPoolExecutors.commonThreadPool().execute(() -> {
                    try {
                        doInSynchronized(lock, action);
                    } catch (Throwable e) {
                        LOG.error("Do synchronized retry error, " + message.get(), e);
                    }
                });
            }
            Threads.interruptIfNecessary(t);
        }
    }

    public static void checkClobMaximumLength(String text, String name) {
        int length = StringUtils.length(text);
        if (length > JobConstants.CLOB_MAXIMUM_LENGTH) {
            throw new IllegalArgumentException(name + " length too large: " + length);
        }
    }

    public static String trimRequired(String text, int maximumLength, String name) {
        if (StringUtils.isBlank(text)) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return trimOptional(text, maximumLength, name);
    }

    public static String trimOptional(String text, int maximumLength, String name) {
        String str = StringUtils.trim(text);
        int length = StringUtils.length(str);
        if (length > maximumLength) {
            throw new IllegalArgumentException(name + " length exceed limit: " + length + " > " + maximumLength);
        }
        return str;
    }

    // ----------------------------------------------------------------------private methods

    private static boolean isValidHost(String host, String from) {
        if (StringUtils.isBlank(host)) {
            return false;
        }
        if (!NetUtils.isValidLocalHost(host)) {
            LOG.warn("Invalid server host configured {}: {}", from, host);
            return false;
        }
        if (!NetUtils.isReachableHost(host)) {
            LOG.warn("Unreachable server host configured {}: {}", from, host);
        }
        return true;
    }

    private static void doInSynchronized(Long lock, ThrowingRunnable<?> action) throws Throwable {
        // Long.toString(lock).intern()
        synchronized (INSTANCE_LOCK_POOL.intern(lock)) {
            action.run();
        }
    }

    private static boolean isCurrentThreadInterrupted(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof java.lang.ThreadDeath || t instanceof InterruptedException) {
            return true;
        }
        Thread curThread = Thread.currentThread();
        return curThread.isInterrupted() || Threads.isStopped(curThread);
    }

}
