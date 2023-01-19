/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.worker.configuration;

import cn.ponfee.scheduler.common.base.ToJsonString;
import cn.ponfee.scheduler.core.base.JobConstants;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;

/**
 * Worker configuration properties.
 *
 * @author Ponfee
 */
@Getter
@Setter
@ConfigurationProperties(prefix = JobConstants.WORKER_KEY_PREFIX)
public class WorkerProperties extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 7914242555106016172L;

    /**
     * Worker group name, default 'default' value.
     */
    private String group = "default";

    /**
     * Worker timing wheel tick milliseconds.
     */
    private long timingWheelTickMs = 100;

    /**
     * Worker timing wheel ring size.
     */
    private int timingWheelRingSize = 60;

    /**
     * Worker maximum pool size, default 100.
     */
    private int maximumPoolSize = 100;

    /**
     * Worker maximum pool size, default 300 seconds.
     */
    private int keepAliveTimeSeconds = 300;

}
