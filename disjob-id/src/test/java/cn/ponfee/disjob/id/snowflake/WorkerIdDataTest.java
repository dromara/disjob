/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.id.snowflake;

import cn.ponfee.disjob.id.snowflake.zk.ZkDistributedSnowflake;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * TODO description
 *
 * @author Ponfee
 */
public class WorkerIdDataTest {
/*

    @Test
    public void test() {
        long currentTime = System.currentTimeMillis();
        String serverTag = "test中文";
        ZkDistributedSnowflake.WorkerIdData data = new ZkDistributedSnowflake.WorkerIdData(currentTime, serverTag);
        byte[] bytes = data.serialize();
        data = ZkDistributedSnowflake.WorkerIdData.deserialize(bytes);
        Assertions.assertEquals(currentTime, data.lastHeartbeatTime);
        Assertions.assertEquals(serverTag, data.serverTag);
    }
*/

}
