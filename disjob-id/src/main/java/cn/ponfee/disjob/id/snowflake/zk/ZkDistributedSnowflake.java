/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.id.snowflake.zk;

import cn.ponfee.disjob.common.base.IdGenerator;
import cn.ponfee.disjob.common.base.RetryTemplate;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.exception.Throwables;
import cn.ponfee.disjob.common.util.ObjectUtils;
import cn.ponfee.disjob.id.snowflake.ClockBackwardsException;
import cn.ponfee.disjob.id.snowflake.Snowflake;
import cn.ponfee.disjob.id.snowflake.db.SnowflakeWorker;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Snowflake server based zookeeper
 *
 * @author Ponfee
 */
public class ZkDistributedSnowflake implements IdGenerator, AutoCloseable {

    private final static Logger LOG = LoggerFactory.getLogger(ZkDistributedSnowflake.class);

    @Override
    public long generateId() {
        return 0;
    }

    @Override
    public void close() throws Exception {

    }
}
