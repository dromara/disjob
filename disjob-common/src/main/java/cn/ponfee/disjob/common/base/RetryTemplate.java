/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.base;

import cn.ponfee.disjob.common.util.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Retry template(template method pattern)
 *
 * @author Ponfee
 */
public class RetryTemplate {

    private static final Logger LOG = LoggerFactory.getLogger(RetryTemplate.class);

    public static <T> T execute(Supplier<T> action, int maxRetryCount, long backOffPeriodMs) throws Throwable {
        int i = 0;
        Throwable ex;
        String traceId = null;
        do {
            try {
                return action.get();
            } catch (Throwable e) {
                ex = e;
                if (i < maxRetryCount) {
                    // log and sleep if not the last loop
                    if (traceId == null) {
                        traceId = ObjectUtils.uuid32();
                    }
                    LOG.error("Execute failed, will retrying: " + (i + 1) + " | " + traceId, e);
                    Thread.sleep(backOffPeriodMs * (i + 1));
                }
            }
        } while (++i <= maxRetryCount);

        throw ex;
    }

}
