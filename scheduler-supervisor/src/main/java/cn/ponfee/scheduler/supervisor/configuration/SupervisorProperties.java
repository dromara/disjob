package cn.ponfee.scheduler.supervisor.configuration;

import cn.ponfee.scheduler.core.base.JobConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Supervisor configuration properties.
 *
 * @author Ponfee
 */
@ConfigurationProperties(prefix = JobConstants.SUPERVISOR_KEY_PREFIX)
@Data
public class SupervisorProperties {

    /**
     * Scan sched_job heartbeat interval seconds, default 2 seconds.
     */
    private int jobHeartbeatIntervalSeconds = 2;

    /**
     * Scan sched_track heartbeat interval seconds, default 2 seconds.
     */
    private int trackHeartbeatIntervalSeconds = 2;
}
