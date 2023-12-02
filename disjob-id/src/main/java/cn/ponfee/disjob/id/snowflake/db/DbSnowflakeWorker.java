/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.id.snowflake.db;

import lombok.Getter;
import lombok.Setter;

/**
 * Snowflake worker
 *
 * @author Ponfee
 */

@Getter
@Setter
public class DbSnowflakeWorker {

    private String bizTag;
    private String serverTag;
    private Integer workerId;
    private Long heartbeatTime;

    public boolean equals(String bizTag, String serverTag) {
        return this.bizTag.equals(bizTag)
            && this.serverTag.equals(serverTag);
    }

}
