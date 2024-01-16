/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

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
@ConfigurationProperties(prefix = JobConstants.SUPERVISOR_KEY_PREFIX)
public class SupervisorProperties extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -7896732123210543684L;

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

    public void check() {
        Assert.isTrue(scanTriggeringJobPeriodMs > 0, "Scan triggering job period ms must be greater than 0.");
        Assert.isTrue(scanWaitingInstancePeriodMs > 0, "Scan waiting instance period ms must be greater than 0.");
        Assert.isTrue(scanRunningInstancePeriodMs > 0, "Scan running instance period ms must be greater than 0.");
        Assert.isTrue(processJobMaximumPoolSize > 0, "Process job maximum pool size must be greater than 0.");
        Assert.isTrue(groupRefreshPeriodSeconds >= 30, "group refresh period seconds cannot less than 30s.");
    }

}
