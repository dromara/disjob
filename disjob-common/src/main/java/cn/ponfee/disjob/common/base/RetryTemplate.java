/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

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
                Thread.currentThread().interrupt();
                throw e;
            } catch (Throwable e) {
                ex = e;
                if (i < retryMaxCount) {
                    // log and sleep if not the last loop
                    if (traceId == null) {
                        traceId = UuidUtils.uuid32();
                    }
                    LOG.error("Execute failed, will retrying: " + (i + 1) + " | " + traceId, e);
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
