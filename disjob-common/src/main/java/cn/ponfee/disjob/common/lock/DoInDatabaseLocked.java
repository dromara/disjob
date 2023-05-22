/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ConnectionCallback;
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

    private static final Logger LOG = LoggerFactory.getLogger(DoInDatabaseLocked.class);

    /**
     * Spring jdbc template.
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Execution lockable sql(use: select * from xxx for update)
     */
    private final String lockSql;

    public DoInDatabaseLocked(JdbcTemplate jdbcTemplate, String lockSql) {
        this.jdbcTemplate = jdbcTemplate;
        this.lockSql = lockSql;
    }

    @Override
    public <T> T action(Callable<T> caller) {
        return jdbcTemplate.execute((ConnectionCallback<T>) connection -> {
            Boolean autoCommit = null;
            PreparedStatement ps = null;
            try {
                // getting the lock until hold
                autoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                ps = connection.prepareStatement(lockSql);
                ps.execute();

                // got the lock, then do callable
                return caller.call();
            } catch (Throwable t) {
                LOG.error("Do in db lock occur error.", t);
                return null;
            } finally {
                try {
                    // release the lock
                    connection.commit();
                } catch (Throwable t) {
                    LOG.error("Commit connection occur error.", t);
                }
                if (autoCommit != null) {
                    try {
                        // restore the auto-commit config
                        connection.setAutoCommit(autoCommit);
                    } catch (Throwable t) {
                        LOG.error("Restore connection auto-commit occur error.", t);
                    }
                }
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (Throwable t) {
                        LOG.error("Close prepare statement occur error.", t);
                    }
                }
            }
        });
    }

}
