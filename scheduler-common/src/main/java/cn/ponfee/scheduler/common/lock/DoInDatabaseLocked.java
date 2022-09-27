package cn.ponfee.scheduler.common.lock;

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
    public <T> T apply(Callable<T> caller) {
        return jdbcTemplate.execute((ConnectionCallback<T>) connection -> {
            Boolean autoCommit = null;
            PreparedStatement ps = null;
            T result = null;
            try {
                // getting the lock until hold
                autoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                ps = connection.prepareStatement(lockSql);
                ps.execute();

                // got the lock, then do callable
                result = caller.call();
            } catch (Exception e) {
                LOG.error("Do in db lock occur error.", e);
            } finally {
                try {
                    // release the lock
                    connection.commit();
                } catch (Exception e) {
                    LOG.error("Commit connection occur error.", e);
                }
                if (autoCommit != null) {
                    try {
                        // restore the auto-commit config
                        connection.setAutoCommit(autoCommit);
                    } catch (Exception e) {
                        LOG.error("Restore connection auto-commit occur error.", e);
                    }
                }
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (Exception e) {
                        LOG.error("Close prepare statement occur error.", e);
                    }
                }
            }

            return result;
        });
    }

}
