/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.util;

import cn.ponfee.scheduler.common.util.NetUtils;
import cn.ponfee.scheduler.core.base.JobConstants;
import org.apache.commons.lang3.StringUtils;

/**
 * Job utility
 *
 * @author Ponfee
 */
public class JobUtils {

    public static String getLocalHost(String specifiedHost) {
        String host = specifiedHost;
        if (StringUtils.isNotEmpty(host)) {
            return validateHost(host, "specified");
        }

        host = System.getProperty(JobConstants.SCHEDULER_BOUND_SERVER_HOST);
        if (StringUtils.isNotEmpty(host)) {
            return validateHost(host, "jvm");
        }

        host = System.getenv(JobConstants.SCHEDULER_BOUND_SERVER_HOST);
        if (StringUtils.isNotEmpty(host)) {
            return validateHost(host, "os");
        }

        host = NetUtils.getLocalHost();
        if (StringUtils.isNotEmpty(host)) {
            return validateHost(host, "network");
        }

        throw new IllegalStateException("Not found available server host");
    }

    private static String validateHost(String host, String from) {
        if (NetUtils.isValidLocalHost(host)) {
            return host;
        }
        throw new AssertionError("Invalid bound server host configured " + from + ": " + host);
    }

}
