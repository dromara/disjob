/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.base;

import cn.ponfee.disjob.common.base.ToJsonString;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * Retry configuration properties.
 *
 * @author Ponfee
 */
@Getter
@Setter
@ConfigurationProperties(prefix = JobConstants.RETRY_KEY_PREFIX)
public class RetryProperties extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -2300492906607942870L;

    /**
     * Retry max count, default 3.
     */
    private int maxCount = 3;

    /**
     * Backoff period, default 5000 ms.
     */
    private int backoffPeriod = 5000;

    public static RetryProperties of(int maxCount, int backoffPeriod) {
        RetryProperties retry = new RetryProperties();
        retry.setMaxCount(maxCount);
        retry.setBackoffPeriod(backoffPeriod);
        return retry;
    }

    public void check() {
        Assert.isTrue(maxCount >= 0, "Retry max count cannot less than 0.");
        Assert.isTrue(backoffPeriod > 0, "Retry backoff period must be greater than 0.");
    }

}
