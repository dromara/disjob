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

package cn.ponfee.disjob.common.base;

import cn.ponfee.disjob.common.concurrent.Threads;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingSupplier;
import cn.ponfee.disjob.common.util.UuidUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * Retry template(template method pattern)
 *
 * @author Ponfee
 */
public class RetryTemplate {

    private static final Logger LOG = LoggerFactory.getLogger(RetryTemplate.class);

    public static void execute(ThrowingRunnable<Throwable> action, int retryMaxCount, long retryBackoffPeriod) throws Throwable {
        execute(action.toSupplier(Boolean.TRUE), retryMaxCount, retryBackoffPeriod);
    }

    public static <T> T execute(ThrowingSupplier<T, Throwable> action, int retryMaxCount, long retryBackoffPeriod) throws Throwable {
        Assert.isTrue(retryMaxCount >= 0, "Retry max count cannot less than 0.");
        Assert.isTrue(retryBackoffPeriod > 0, "Retry backoff period must be greater than 0.");
        int i = 0;
        Throwable ex;
        String traceId = null;
        do {
            try {
                return action.get();
            } catch (InterruptedException e) {
                LOG.error("Thread interrupted, skip retry.");
                throw e;
            } catch (Throwable e) {
                ex = e;
                if (i < retryMaxCount) {
                    // log and sleep if not the last loop
                    if (traceId == null) {
                        traceId = UuidUtils.uuid32();
                    }
                    LOG.error("Execute failed, will retrying: " + (i + 1) + ", " + traceId, e);
                    Thread.sleep((i + 1) * retryBackoffPeriod);
                } else {
                    LOG.error("Execute failed, retried max count: " + traceId, e);
                }
            }
        } while (++i <= retryMaxCount);

        throw ex;
    }

    public static void executeQuietly(ThrowingRunnable<Throwable> action, int retryMaxCount, long retryBackoffPeriod) {
        executeQuietly(action.toSupplier(Boolean.TRUE), retryMaxCount, retryBackoffPeriod);
    }

    public static <T> T executeQuietly(ThrowingSupplier<T, Throwable> action, int retryMaxCount, long retryBackoffPeriod) {
        try {
            return execute(action, retryMaxCount, retryBackoffPeriod);
        } catch (Throwable t) {
            Threads.interruptIfNecessary(t);
            return null;
        }
    }

}
