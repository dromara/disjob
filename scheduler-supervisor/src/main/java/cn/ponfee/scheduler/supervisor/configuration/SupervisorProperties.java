/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.configuration;

import cn.ponfee.scheduler.common.base.ToJsonString;
import cn.ponfee.scheduler.core.base.JobConstants;
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
     * Scan waiting track period milliseconds
     */
    private long scanWaitingTrackPeriodMs = 5000;

    /**
     * Scan running track period milliseconds
     */
    private long scanRunningTrackPeriodMs = 30000;

    /**
     * Force use local worker service client.
     */
    private boolean forceLocalWorkerService = false;

    public void check() {
        Assert.isTrue(scanTriggeringJobPeriodMs > 0, "Scan triggering job period ms must be greater than 0.");
        Assert.isTrue(scanWaitingTrackPeriodMs > 0, "Scan waiting track period ms must be greater than 0.");
        Assert.isTrue(scanRunningTrackPeriodMs > 0, "Scan running track period ms must be greater than 0.");
    }

}
