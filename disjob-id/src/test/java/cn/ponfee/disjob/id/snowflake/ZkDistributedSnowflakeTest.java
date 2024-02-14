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

package cn.ponfee.disjob.id.snowflake;

import cn.ponfee.disjob.id.snowflake.zk.ZkConfig;
import cn.ponfee.disjob.id.snowflake.zk.ZkDistributedSnowflake;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * ZkDistributedSnowflakeTest
 *
 * @author Ponfee
 */
@Disabled
public class ZkDistributedSnowflakeTest {

    @Test
    public void test() {
        ZkConfig zkConfig = new ZkConfig();
        zkConfig.setConnectString("localhost:2181");
        ZkDistributedSnowflake snowflake = new ZkDistributedSnowflake(zkConfig, "disjob", "app1:8080");
        new ZkDistributedSnowflake(zkConfig, "disjob", "app2:8080");
        new ZkDistributedSnowflake(zkConfig, "disjob", "app2:8080");

        for (int i = 0; i < 5; i++) {
            System.out.println(snowflake.generateId());
        }
    }
}
