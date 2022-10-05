package cn.ponfee.scheduler.registry.consul.configuration;

import cn.ponfee.scheduler.core.base.JobConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Consul configuration properties.
 *
 * @author Ponfee
 */
@ConfigurationProperties(prefix = JobConstants.SCHEDULER_KEY_PREFIX + ".consul")
@Data
public class ConsulProperties {

    /**
     * Consul client host
     */
    private String host;

    /**
     * Consul client port
     */
    private int port;

    /**
     * Consul token
     */
    private String token;

}
