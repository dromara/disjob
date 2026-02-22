/*
 * Copyright 2022-2026 Ponfee (http://www.ponfee.cn/)
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

package cn.ponfee.disjob.common.base;

import cn.ponfee.disjob.common.concurrent.Threads;
import cn.ponfee.disjob.common.exception.Throwables;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingSupplier;
import cn.ponfee.disjob.common.util.UuidUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * Retry template(template method pattern)
 * <pre>
 *  Others retry framework:
 *    org.springframework.retry:spring-retry:2.0.7
 *    com.github.rholder:guava-retrying:2.0.0
 *    dev.failsafe:failsafe:3.3.2
 *    io.github.resilience4j:resilience4j-retry:2.2.0
 * </pre>
 *
 * @author Ponfee
 */
public class RetryTemplate {

    private static final Logger LOG = LoggerFactory.getLogger(RetryTemplate.class);

    public static void execute(ThrowingRunnable<Throwable> action, int retryMaxCount, long retryBackoffPeriod) {
        execute(action.toSupplier(null), retryMaxCount, retryBackoffPeriod);
    }

    public static <T> T execute(ThrowingSupplier<T, Throwable> action, int retryMaxCount, long retryBackoffPeriod) {
        Assert.isTrue(retryMaxCount >= 0, "Retry max count cannot less than 0.");
        Assert.isTrue(retryBackoffPeriod > 0, "Retry backoff period must be greater than 0.");
        Throwable throwable = null;
        String traceId = null;
        for (int i = 0; i <= retryMaxCount; i++) {
            try {
                return action.get();
            } catch (Throwable t) {
                Throwables.rethrowIfFatal(t);
                if (throwable == null) {
                    throwable = t;
                }
                if (traceId == null) {
                    traceId = UuidUtils.uuid32();
                }
                if (i < retryMaxCount) {
                    // log and sleep if not the last loop
                    LOG.error("Execute failed will retry: {}, {}", i + 1, traceId, t);
                    Threads.sleep((i + 1) * retryBackoffPeriod);
                } else {
                    LOG.error("Execute failed will exit: {}, {}", i + 1, traceId, t);
                }
            }
        }
        return ExceptionUtils.rethrow(throwable);
    }

    public static void executeQuietly(ThrowingRunnable<Throwable> action, int retryMaxCount, long retryBackoffPeriod) {
        executeQuietly(action.toSupplier(null), retryMaxCount, retryBackoffPeriod);
    }

    public static <T> T executeQuietly(ThrowingSupplier<T, Throwable> action, int retryMaxCount, long retryBackoffPeriod) {
        Assert.isTrue(retryMaxCount >= 0, "Retry max count cannot less than 0.");
        Assert.isTrue(retryBackoffPeriod > 0, "Retry backoff period must be greater than 0.");
        String traceId = null;
        for (int i = 0; i <= retryMaxCount; i++) {
            try {
                return action.get();
            } catch (Throwable t) {
                Throwables.rethrowIfFatal(t);
                if (traceId == null) {
                    traceId = UuidUtils.uuid32();
                }
                if (i < retryMaxCount) {
                    LOG.error("Execute failed quietly retry: {}, {}", i + 1, traceId, t);
                    Threads.sleep((i + 1) * retryBackoffPeriod);
                } else {
                    LOG.error("Execute failed quietly exit: {}, {}", i + 1, traceId, t);
                }
            }
        }
        return null;
    }

}
