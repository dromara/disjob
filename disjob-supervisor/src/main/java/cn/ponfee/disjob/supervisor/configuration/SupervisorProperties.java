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
@ConfigurationProperties(prefix = SupervisorProperties.KEY_PREFIX)
public class SupervisorProperties extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -7896732123210543684L;
    public static final String KEY_PREFIX = JobConstants.DISJOB_KEY_PREFIX + ".supervisor";

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
     * Process job maximum thread pool size, default 5
     */
    private int processJobMaximumPoolSize = 5;

    /**
     * Group data refresh period seconds.
     */
    private int groupRefreshPeriodSeconds = 120;

    /**
     * Job scan failed count threshold.
     */
    private int jobScanFailedCountThreshold = 5;

    /**
     * Task dispatch failed count threshold.
     */
    private int taskDispatchFailedCountThreshold = 3;

    public void check() {
        Assert.isTrue(scanTriggeringJobPeriodMs > 0, "Scan triggering job period ms must be greater than 0.");
        Assert.isTrue(scanWaitingInstancePeriodMs > 0, "Scan waiting instance period ms must be greater than 0.");
        Assert.isTrue(scanRunningInstancePeriodMs > 0, "Scan running instance period ms must be greater than 0.");
        Assert.isTrue(processJobMaximumPoolSize > 0, "Process job maximum pool size must be greater than 0.");
        Assert.isTrue(groupRefreshPeriodSeconds >= 30, "group refresh period seconds cannot less than 30s.");
        Assert.isTrue(jobScanFailedCountThreshold >= 0, "Job scan failed count threshold cannot less than 0.");
        Assert.isTrue(taskDispatchFailedCountThreshold >= 0, "Task dispatch failed count threshold cannot less than 0.");
    }

}
