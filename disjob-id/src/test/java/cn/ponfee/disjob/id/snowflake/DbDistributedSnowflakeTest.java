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

import cn.ponfee.disjob.id.snowflake.db.DbDistributedSnowflake;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * DatabaseSnowflakeServerTest
 *
 * @author Ponfee
 */
public class DbDistributedSnowflakeTest {

    @Test
    public void test() {
        JdbcTemplate mock = Mockito.mock(JdbcTemplate.class);
        when(mock.execute((ConnectionCallback<Boolean>) any())).thenReturn(true);
        DbDistributedSnowflake snowflake = new DbDistributedSnowflake(mock, "disjob", "app1:8080");
        System.out.println(snowflake.generateId());
    }
}
