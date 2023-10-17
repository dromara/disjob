/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

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
