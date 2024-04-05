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

package cn.ponfee.disjob.core.base;

import cn.ponfee.disjob.common.base.ToJsonString;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * Http configuration properties.
 *
 * @author Ponfee
 */
@Getter
@Setter
@ConfigurationProperties(prefix = HttpProperties.KEY_PREFIX)
public class HttpProperties extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 5956808059609905150L;
    public static final String KEY_PREFIX = JobConstants.DISJOB_KEY_PREFIX + ".http";

    /**
     * Http rest connect timeout milliseconds, default 2000.
     */
    private int connectTimeout = 2000;

    /**
     * Http rest read timeout milliseconds, default 5000.
     */
    private int readTimeout = 5000;

    public void check() {
        Assert.isTrue(connectTimeout > 0, "Http connect timeout must be greater than 0.");
        Assert.isTrue(readTimeout > 0, "Http read timeout must be greater than 0.");
    }

}
