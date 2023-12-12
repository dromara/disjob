/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.worker.configuration;

import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.core.base.JobConstants;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

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
     * Worker group name
     */
    private String group;

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

    /**
     * Process thread pool size, default 5.
     */
    private int processThreadPoolSize = 5;

    /**
     * The token which use call rpc to supervisor
     */
    private String workerToken;

    /**
     * The token which is from supervisor rpc call
     */
    private String supervisorToken;

    public void check() {
        Assert.hasText(group, "Group cannot be blank.");
        Assert.isTrue(timingWheelTickMs > 0, "Timing wheel tick ms must be greater than 0.");
        Assert.isTrue(timingWheelRingSize > 0, "Timing wheel ring size must be greater than 0.");
        Assert.isTrue(maximumPoolSize > 0, "Maximum pool size must be greater 0.");
        Assert.isTrue(keepAliveTimeSeconds > 0, "Keep alive time seconds must be greater 0.");
        Assert.isTrue(processThreadPoolSize > 0, "Process thread pool size must be greater than 0.");
    }

}
