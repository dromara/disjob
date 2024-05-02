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

package cn.ponfee.disjob.core.util;

import cn.ponfee.disjob.common.util.NetUtils;
import cn.ponfee.disjob.core.base.JobConstants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Disjob utility
 *
 * @author Ponfee
 */
public class DisjobUtils {

    private static final Logger LOG = LoggerFactory.getLogger(DisjobUtils.class);

    public static String getLocalHost(String specifiedHost) {
        String host = specifiedHost;
        if (isValidHost(host, "specified")) {
            return host;
        }

        host = System.getProperty(JobConstants.DISJOB_BOUND_SERVER_HOST);
        if (isValidHost(host, "System#getProperty")) {
            return host;
        }

        host = System.getenv(JobConstants.DISJOB_BOUND_SERVER_HOST);
        if (isValidHost(host, "System#getenv")) {
            return host;
        }

        host = NetUtils.getLocalHost();
        if (isValidHost(host, "NetUtils#getLocalHost")) {
            return host;
        }

        throw new Error("Not found available server host.");
    }

    private static boolean isValidHost(String host, String from) {
        if (StringUtils.isBlank(host)) {
            return false;
        }
        if (!NetUtils.isValidLocalHost(host)) {
            LOG.warn("Invalid server host configured {}: {}", from, host);
            return false;
        }
        if (!NetUtils.isReachableHost(host)) {
            LOG.warn("Unreachable server host configured {}: {}", from, host);
        }
        return true;
    }

}
