/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.id.snowflake.zk;

import lombok.Getter;
import lombok.Setter;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Zookeeper config
 *
 * @author Ponfee
 */
@Getter
@Setter
public class ZkConfig implements java.io.Serializable {
    private static final long serialVersionUID = -2679723559759163644L;

    private String connectString = "localhost:2181";
    private String username;
    private String password;

    private int connectionTimeoutMs = 5 * 1000;
    private int sessionTimeoutMs = 5 * 60 * 1000;

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
