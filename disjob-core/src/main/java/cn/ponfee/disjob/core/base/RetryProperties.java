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
@ConfigurationProperties(prefix = JobConstants.RETRY_CONFIG_KEY)
public class RetryProperties extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -2300492906607942870L;

    /**
     * Retry max count, default 3.
     */
    private int maxCount = 3;

    /**
     * Backoff period milliseconds, default 3000.
     */
    private long backoffPeriod = 3000;

    public static RetryProperties none() {
        return of(0, 0);
    }

    public static RetryProperties of(int maxCount, int backoffPeriod) {
        RetryProperties retry = new RetryProperties();
        retry.setMaxCount(maxCount);
        retry.setBackoffPeriod(backoffPeriod);
        retry.check();
        return retry;
    }

    public void check() {
        Assert.isTrue(maxCount >= 0, "Retry max count cannot less than 0.");
        if (maxCount == 0) {
            Assert.isTrue(backoffPeriod == 0, "Retry backoff period must be 0.");
        } else {
            Assert.isTrue(backoffPeriod > 0, "Retry backoff period must be greater than 0.");
        }
    }

}
