/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.base;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Retry invocation handler
 *
 * @author Ponfee
 */
public class RetryInvocationHandler implements InvocationHandler {

    private final Object target;
    private final int retryMaxCount;
    private final long retryBackoffPeriod;

    public RetryInvocationHandler(Object target, int retryMaxCount, int retryBackoffPeriod) {
        this.target = Objects.requireNonNull(target, "Target object cannot be null.");
        this.retryMaxCount = retryMaxCount;
        this.retryBackoffPeriod = retryBackoffPeriod;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return RetryTemplate.execute(() -> method.invoke(target, args), retryMaxCount, retryBackoffPeriod);
    }

}
