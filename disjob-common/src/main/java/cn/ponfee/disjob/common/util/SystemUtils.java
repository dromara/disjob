/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * System utility.
 *
 * @author Ponfee
 */
public final class SystemUtils {

    private static final Logger LOG = LoggerFactory.getLogger(SystemUtils.class);

    public static String getConfig(String name) {
        String value = null;
        try {
            value = System.getProperty(name);
            if (StringUtils.isNotEmpty(value)) {
                return value;
            }
        } catch (Exception e) {
            LOG.error("Get system property occur error: " + name, e);
        }

        try {
            value = System.getenv(name);
            if (StringUtils.isNotEmpty(value)) {
                return value;
            }
        } catch (Exception e) {
            LOG.error("Get system env occur error: " + name, e);
        }

        return value;
    }

    public static String getConfig(String name, String defaultValue) {
        String value = getConfig(name);
        return value != null ? value : defaultValue;
    }

}
