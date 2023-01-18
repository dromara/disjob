/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.configuration;

import cn.ponfee.scheduler.core.base.JobConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * Supervisor configuration properties.
 *
 * @author Ponfee
 */
@ConfigurationProperties(prefix = JobConstants.SUPERVISOR_KEY_PREFIX)
@Data
public class SupervisorProperties {

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

    public void check() {
        Assert.isTrue(scanTriggeringJobPeriodMs > 0, "Scan triggering job period ms must be greater than 0.");
        Assert.isTrue(scanWaitingTrackPeriodMs > 0, "Scan waiting track period ms must be greater than 0.");
        Assert.isTrue(scanRunningTrackPeriodMs > 0, "Scan running track period ms must be greater than 0.");
    }

}
