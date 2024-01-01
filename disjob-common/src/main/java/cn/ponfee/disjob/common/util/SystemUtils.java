/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import org.apache.commons.lang3.StringUtils;

/**
 * System utility.
 *
 * @author Ponfee
 */
public final class SystemUtils {

    public static String getConfig(String name) {
        String value = System.getProperty(name);
        if (StringUtils.isNotEmpty(value)) {
            return value;
        }

        return System.getenv(name);
    }

}
