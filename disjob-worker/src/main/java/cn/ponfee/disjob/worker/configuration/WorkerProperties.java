/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.worker.configuration;

import cn.ponfee.disjob.common.base.Symbol.Str;
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
@ConfigurationProperties(prefix = WorkerProperties.KEY_PREFIX)
public class WorkerProperties extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 7914242555106016172L;
    public static final String KEY_PREFIX = JobConstants.DISJOB_KEY_PREFIX + ".worker";

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

    /**
     * Supervisor context-path
     */
    private String supervisorContextPath = "/";

    public void check() {
        Assert.hasText(group, "Group cannot be blank.");
        Assert.isTrue(timingWheelTickMs > 0, "Timing wheel tick ms must be greater than 0.");
        Assert.isTrue(timingWheelRingSize > 0, "Timing wheel ring size must be greater than 0.");
        Assert.isTrue(maximumPoolSize > 0, "Maximum pool size must be greater 0.");
        Assert.isTrue(keepAliveTimeSeconds > 0, "Keep alive time seconds must be greater 0.");
        Assert.isTrue(processThreadPoolSize > 0, "Process thread pool size must be greater than 0.");
        Assert.isTrue(supervisorContextPath.startsWith(Str.SLASH), () -> "Supervisor context-path must start with '/': " + supervisorContextPath);
        if (supervisorContextPath.length() > 1) {
            Assert.isTrue(!supervisorContextPath.endsWith(Str.SLASH), "Supervisor context-path cannot end with '/': " + supervisorContextPath);
        }
    }

}
