package cn.ponfee.scheduler.worker.base;

import cn.ponfee.scheduler.core.base.JobConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Worker configuration properties.
 *
 * @author Ponfee
 */
@ConfigurationProperties(prefix = JobConstants.WORKER_KEY_PREFIX)
@Data
public class WorkerProperties {

    /**
     * Worker group name, default 'default' value.
     */
    private String group = "default";

    /**
     * Worker maximum pool size, default 100.
     */
    private int maximumPoolSize = 100;

    /**
     * Worker maximum pool size, default 300 seconds.
     */
    private int keepAliveTimeSeconds = 300;

}
