package cn.ponfee.scheduler.core.base;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Worker configuration properties.
 *
 * @author Ponfee
 */
@ConfigurationProperties(prefix = JobConstants.HTTP_KEY_PREFIX)
@Data
public class HttpProperties {

    /**
     * Http rest connect timeout milliseconds, default 2000.
     */
    private int connectTimeout = 2000;

    /**
     * Http rest read timeout milliseconds, default 5000.
     */
    private int readTimeout = 5000;

    /**
     * Http rest max retry times, default 3.
     */
    private int maxRetryTimes = 3;

}
