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

package cn.ponfee.disjob.supervisor.configuration;

import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.core.base.JobConstants;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * Supervisor configuration properties.
 *
 * @author Ponfee
 */
@Getter
@Setter
@ConfigurationProperties(prefix = JobConstants.SUPERVISOR_CONFIG_KEY)
public class SupervisorProperties extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -7896732123210543684L;

    /**
     * Scan batch size
     */
    private int scanBatchSize = 200;

    /**
     * Maximum split task size
     */
    private int maximumSplitTaskSize = 1000;

    /**
     * Maximum job depends depth
     */
    private int maximumJobDependsDepth = 20;

    /**
     * Maximum job retry count
     */
    private int maximumJobRetryCount = 5;

    /**
     * Scan triggering job period milliseconds
     */
    private long scanTriggeringJobPeriodMs = 3000;

    /**
     * Scan waiting instance period milliseconds
     */
    private long scanWaitingInstancePeriodMs = 15000;

    /**
     * Scan running instance period milliseconds
     */
    private long scanRunningInstancePeriodMs = 30000;

    /**
     * Shutdown task delay resume milliseconds
     */
    private long shutdownTaskDelayResumeMs = 600000;

    /**
     * Maximum process job thread pool size, default 10
     */
    private int maximumProcessJobPoolSize = 10;

    /**
     * Group data refresh period minutes.
     */
    private int groupRefreshPeriodMinutes = 0;

    /**
     * Job scan failed count threshold.
     */
    private int jobScanFailedCountThreshold = 5;

    /**
     * Task dispatch failed count threshold.
     */
    private int taskDispatchFailedCountThreshold = 3;

    public void check() {
        Assert.isTrue(20 <= scanBatchSize && scanBatchSize <= 2000, "Scan batch size must be range [20, 2000].");
        Assert.isTrue(maximumSplitTaskSize > 0, "Maximum split task size must be greater than 0.");
        Assert.isTrue(0 < maximumJobDependsDepth && maximumJobDependsDepth < 100, "Maximum job depends depth must be range [1, 99].");
        Assert.isTrue(0 < maximumJobRetryCount && maximumJobRetryCount < 10, "Maximum job retry count must be range [1, 9].");
        Assert.isTrue(scanTriggeringJobPeriodMs >= 1000, "Scan triggering job period ms cannot less than 1000.");
        Assert.isTrue(scanWaitingInstancePeriodMs >= 15000, "Scan waiting instance period ms cannot less than 15000.");
        Assert.isTrue(scanRunningInstancePeriodMs >= 30000, "Scan running instance period ms cannot less than 30000.");
        Assert.isTrue(shutdownTaskDelayResumeMs >= 60000, "Shutdown task delay resume ms cannot less than 60000.");
        Assert.isTrue(maximumProcessJobPoolSize > 0, "Maximum process job pool size must be greater than 0.");
        Assert.isTrue(groupRefreshPeriodMinutes >= 0, "Refresh group period minutes cannot less than 0.");
        Assert.isTrue(jobScanFailedCountThreshold >= 0, "Job scan failed count threshold cannot less than 0.");
        Assert.isTrue(taskDispatchFailedCountThreshold >= 0, "Task dispatch failed count threshold cannot less than 0.");
    }

}
