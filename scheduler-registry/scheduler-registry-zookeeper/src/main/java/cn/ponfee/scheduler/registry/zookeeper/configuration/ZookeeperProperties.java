package cn.ponfee.scheduler.registry.zookeeper.configuration;

import cn.ponfee.scheduler.common.base.ToJsonString;
import cn.ponfee.scheduler.core.base.JobConstants;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Zookeeper configuration properties.
 *
 * @author Ponfee
 */
@ConfigurationProperties(prefix = ZookeeperProperties.ZOOKEEPER_KEY_PREFIX)
@Getter
@Setter
public class ZookeeperProperties extends ToJsonString implements java.io.Serializable {
    private static final long serialVersionUID = -8395535372974631095L;

    public static final String ZOOKEEPER_KEY_PREFIX = JobConstants.SCHEDULER_KEY_PREFIX + ".zookeeper";

    private String connectString = "localhost:2181";
    private String username;
    private String password;

    private int connectionTimeoutMs = 5 * 1000;
    private int sessionTimeoutMs = 60 * 1000;

    private int baseSleepTimeMs = 50;
    private int maxRetries = 10;
    private int maxSleepMs = 500;
    private int maxWaitTimeMs = 5000;

    public String authorization() {
        if (isEmpty(username)) {
            return isEmpty(password) ? null : ":" + password;
        }
        return username + ":" + (isEmpty(password) ? "" : password);
    }

}
