/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.id.snowflake.database;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Snowflake worker
 *
 * @author Ponfee
 */

@Getter
@Setter
@NoArgsConstructor
public class SnowflakeWorker {

    private String bizTag;
    private String serverTag;
    private Integer workerId;
    private Long heartbeatTime;

    public SnowflakeWorker(String bizTag, String serverTag, Integer workerId, Long heartbeatTime) {
        this.bizTag = bizTag;
        this.serverTag = serverTag;
        this.workerId = workerId;
        this.heartbeatTime = heartbeatTime;
    }

    public boolean equals(String bizTag, String serverTag) {
        return this.bizTag.equals(bizTag)
            && this.serverTag.equals(serverTag);
    }

}
