/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.id.snowflake;

import cn.ponfee.disjob.id.snowflake.zk.ZkDistributedSnowflake;
import cn.ponfee.disjob.id.snowflake.zk.ZookeeperConfig;
import org.junit.jupiter.api.Test;

/**
 * ZkDistributedSnowflakeTest
 *
 * @author Ponfee
 */
public class ZkDistributedSnowflakeTest {

    @Test
    public void test() {
        ZookeeperConfig zkConfig = new ZookeeperConfig();
        zkConfig.setConnectString("localhost:2181");
        ZkDistributedSnowflake snowflake = new ZkDistributedSnowflake(zkConfig, "disjob", "app1:8080");
        new ZkDistributedSnowflake(zkConfig, "disjob", "app2:8080");
        new ZkDistributedSnowflake(zkConfig, "disjob", "app2:8080");

        for (int i = 0; i < 100; i++) {
            System.out.println(snowflake.generateId());
        }
    }
}
