/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.lock;

import cn.ponfee.disjob.common.spring.JdbcTemplateWrapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.util.concurrent.Callable;

/**
 * Distributed lock based jdbc database.<br/>
 * {@code SELECT * FROM xxx FOR UPDATE}
 *
 * @author Ponfee
 */
public final class DoInDatabaseLocked implements DoInLocked {

    /**
     * Spring jdbc template wrapper.
     */
    private final JdbcTemplateWrapper jdbcTemplateWrapper;

    /**
     * Execution lockable sql(use: select * from xxx for update)
     */
    private final String lockSql;

    public DoInDatabaseLocked(JdbcTemplate jdbcTemplate, String lockSql) {
        this.jdbcTemplateWrapper = JdbcTemplateWrapper.of(jdbcTemplate);
        this.lockSql = lockSql;
    }

    @Override
    public <T> T action(Callable<T> caller) {
        return jdbcTemplateWrapper.executeInTransaction(psCreator -> {
            PreparedStatement preparedStatement = psCreator.apply(lockSql);
            preparedStatement.execute();
            return caller.call();
        });
    }

}
