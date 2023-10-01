/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry.database;

import cn.ponfee.disjob.common.base.LoopProcessThread;
import cn.ponfee.disjob.common.base.RetryTemplate;
import cn.ponfee.disjob.common.concurrent.NamedThreadFactory;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.concurrent.Threads;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingSupplier;
import cn.ponfee.disjob.common.spring.JdbcTemplateWrapper;
import cn.ponfee.disjob.common.util.ObjectUtils;
import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.registry.ServerRegistry;
import cn.ponfee.disjob.registry.database.configuration.DatabaseRegistryProperties;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.util.Assert;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static cn.ponfee.disjob.common.spring.JdbcTemplateWrapper.AFFECTED_ONE_ROW;

/**
 * Registry server based database.
 *
 * @author Ponfee
 */
public abstract class DatabaseServerRegistry<R extends Server, D extends Server> extends ServerRegistry<R, D> {

    private static final long DEAD_TIME_MILLIS = TimeUnit.HOURS.toMillis(12);
    private static final RowMapper<String> ROW_MAPPER = new SingleColumnRowMapper<>(String.class);
    private static final String TABLE_NAME = "disjob_registry";

    private static final String CREATE_TABLE_DDL =
        "CREATE TABLE IF NOT EXISTS `" + TABLE_NAME + "` (                                                                    \n" +
        "  `id`              BIGINT        UNSIGNED  NOT NULL  AUTO_INCREMENT  COMMENT 'auto increment id',                   \n" +
        "  `namespace`       VARCHAR(60)             NOT NULL                  COMMENT 'registry namespace',                  \n" +
        "  `role`            VARCHAR(30)             NOT NULL                  COMMENT 'server role',                         \n" +
        "  `server`          VARCHAR(255)            NOT NULL                  COMMENT 'server serialization',                \n" +
        "  `heartbeat_time`  BIGINT        UNSIGNED  NOT NULL                  COMMENT 'last heartbeat time',                 \n" +
        "  PRIMARY KEY (`id`),                                                                                                \n" +
        "  UNIQUE KEY `uk_namespace_role_server` (`namespace`, `role`, `server`),                                             \n" +
        ") ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Registry center for database'; \n" ;

    private static final String REMOVE_DEAD_SQL = "DELETE FROM " + TABLE_NAME + " WHERE namespace=? AND role=? AND heartbeat_time<?";

    private static final String REGISTER_SQL = "INSERT INTO " + TABLE_NAME + " (namespace, role, server, heartbeat_time) VALUES (?, ?, ?, ?)";

    private static final String HEARTBEAT_SQL = "UPDATE " + TABLE_NAME + " SET heartbeat_time=? WHERE namespace=? AND role=? AND server=?";

    private static final String DEREGISTER_SQL = "DELETE FROM " + TABLE_NAME + " WHERE namespace=? AND role=? AND server=?";

    private static final String DISCOVER_SQL = "SELECT server FROM " + TABLE_NAME + " WHERE namespace=? AND role=? AND heartbeat_time>=?";

    /**
     * Registry namespace
     */
    private final String namespace;

    /**
     * Spring jdbc template wrapper
     */
    private final JdbcTemplateWrapper jdbcTemplateWrapper;

    // -------------------------------------------------Registry

    private final String registerRoleName;
    private final LoopProcessThread heartbeatThread;

    // -------------------------------------------------Discovery

    private final long sessionTimeoutMs;
    private final String discoveryRoleName;
    private final ThreadPoolExecutor asyncRefreshExecutor;
    private volatile long nextRefreshTimeMillis = 0;

    protected DatabaseServerRegistry(JdbcTemplate jdbcTemplate,
                                     DatabaseRegistryProperties config) {
        super(config.getNamespace(), ':');
        this.namespace = config.getNamespace().trim();
        this.jdbcTemplateWrapper = JdbcTemplateWrapper.of(jdbcTemplate);

        // registry
        this.registerRoleName = registryRole.name().toLowerCase();

        try {
            RetryTemplate.execute(() -> jdbcTemplateWrapper.createTableIfNotExists(TABLE_NAME, CREATE_TABLE_DDL), 3, 1000L);
        } catch (Throwable e) {
            Threads.interruptIfNecessary(e);
            throw new IllegalStateException("Create " + TABLE_NAME + " table failed.", e);
        }

        Object[] args = {namespace, registerRoleName, System.currentTimeMillis() - DEAD_TIME_MILLIS};
        try {
            RetryTemplate.execute(() -> jdbcTemplateWrapper.delete(REMOVE_DEAD_SQL, args), 3, 1000L);
        } catch (Throwable e) {
            Threads.interruptIfNecessary(e);
            log.error("Remove dead server failed: " + Arrays.toString(args), e);
        }

        this.heartbeatThread = new LoopProcessThread("database_registry_heartbeat", config.getRegistryPeriodMs(), this::heartbeat);
        heartbeatThread.start();

        // discovery
        this.sessionTimeoutMs = config.getSessionTimeoutMs();
        this.discoveryRoleName = discoveryRole.name().toLowerCase();
        this.asyncRefreshExecutor = ThreadPoolExecutors.builder()
            .corePoolSize(1)
            .maximumPoolSize(1)
            .workQueue(new SynchronousQueue<>())
            .keepAliveTimeSeconds(300)
            .rejectedHandler(ThreadPoolExecutors.DISCARD)
            .threadFactory(NamedThreadFactory.builder().prefix("database_discovery_async").priority(Thread.MAX_PRIORITY).build())
            .build();

        try {
            doRefreshDiscoveryServers();
        } catch (Throwable e) {
            Threads.interruptIfNecessary(e);
            close();
            throw new Error("Database registry init discovery error.", e);
        }
    }

    @Override
    public boolean isConnected() {
        return jdbcTemplateWrapper != null;
    }

    // ------------------------------------------------------------------redis的pub/sub并不是很可靠，所以这里去定时刷新

    @Override
    public final List<D> getDiscoveredServers(String group) {
        asyncRefreshDiscoveryServers();
        return super.getDiscoveredServers(group);
    }

    @Override
    public final boolean hasDiscoveredServers() {
        asyncRefreshDiscoveryServers();
        return super.hasDiscoveredServers();
    }

    @Override
    public final boolean isDiscoveredServer(D server) {
        asyncRefreshDiscoveryServers();
        return super.isDiscoveredServer(server);
    }

    // ------------------------------------------------------------------Registry

    @Override
    public final void register(R server) {
        if (closed.get()) {
            return;
        }

        List<R> servers = new ArrayList<>();
        if (server instanceof Worker) {
            for (Worker worker : ((Worker) server).splitGroup()) {
                servers.add((R) worker);
            }
        } else {
            servers.add(server);
        }

        jdbcTemplateWrapper.executeInTransaction(action -> {
            for (R svr : servers) {
                PreparedStatement update = action.apply(HEARTBEAT_SQL);
                update.setLong(1, System.currentTimeMillis());
                update.setString(2, namespace);
                update.setString(3, registerRoleName);
                update.setString(4, server.serialize());
                int updateRowsAffected = update.executeUpdate();
                Assert.isTrue(updateRowsAffected <= AFFECTED_ONE_ROW, () -> "Invalid insert rows affected: " + updateRowsAffected);
                if (updateRowsAffected == AFFECTED_ONE_ROW) {
                    log.info("Database registry register update: {} | {} | {}", namespace, registerRoleName, svr.serialize());
                    continue;
                }

                PreparedStatement insert = action.apply(REGISTER_SQL);
                insert.setString(1, namespace);
                insert.setString(2, registerRoleName);
                insert.setString(3, svr.serialize());
                insert.setLong(4, System.currentTimeMillis());
                int insertRowsAffected = insert.executeUpdate();
                Assert.isTrue(insertRowsAffected == AFFECTED_ONE_ROW, () -> "Invalid insert rows affected: " + insertRowsAffected);
                log.info("Database registry register insert: {} | {} | {}", namespace, insertRowsAffected, svr.serialize());
            }

            return null;
        });
    }

    @Override
    public final void deregister(R server) {
        registered.remove(server);
        Object[] args = new Object[]{namespace, registerRoleName, server.serialize()};
        ThrowingSupplier.execute(() -> jdbcTemplateWrapper.delete(DEREGISTER_SQL, args));
        log.info("Server deregister: {} | {}", registryRole.name(), server);
    }

    // ------------------------------------------------------------------Close

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            log.warn("Repeat call close method\n{}", ObjectUtils.getStackTrace());
            return;
        }

        heartbeatThread.terminate();
        registered.forEach(this::deregister);
        registered.clear();
        ThrowingSupplier.execute(() -> ThreadPoolExecutors.shutdown(asyncRefreshExecutor, 1));
        super.close();
    }

    // ------------------------------------------------------------------private methods

    private void heartbeat() {
        for (R svr : registered) {
            try {
                RetryTemplate.execute(() -> doHeartbeat(svr), 3, 3000L);
            } catch (Throwable t) {
                Threads.interruptIfNecessary(t);
                log.error("Do database registry heartbeat occur error: " + svr.serialize(), t);
            }
        }
    }

    public void doHeartbeat(R server) {
        Object[] updateArgs = {System.currentTimeMillis(), namespace, registerRoleName, server.serialize()};
        if (jdbcTemplateWrapper.update(HEARTBEAT_SQL, updateArgs) == AFFECTED_ONE_ROW) {
            log.info("Database registry heartbeat insert: {} | {} | {} | {}", updateArgs);
            return;
        }

        Object[] insertArgs = {namespace, registerRoleName, server.serialize(), System.currentTimeMillis()};
        jdbcTemplateWrapper.insert(REGISTER_SQL, insertArgs);
        log.info("Database registry heartbeat update: {} | {} | {} | {}", insertArgs);
    }

    private void asyncRefreshDiscoveryServers() {
        if (requireRefresh()) {
            asyncRefreshExecutor.execute(ThrowingRunnable.caught(this::doRefreshDiscoveryServers));
        }
    }

    private void doRefreshDiscoveryServers() throws Throwable {
        if (closed.get() || !requireRefresh()) {
            return;
        }

        RetryTemplate.execute(() -> {
            Object[] args = {namespace, discoveryRoleName, System.currentTimeMillis() - sessionTimeoutMs};
            List<String> discovered = jdbcTemplateWrapper.queryForList(ROW_MAPPER, DISCOVER_SQL, args);

            if (CollectionUtils.isEmpty(discovered)) {
                log.warn("Not discovered available {} from database.", discoveryRole.name());
                discovered = Collections.emptyList();
            }

            List<D> servers = discovered.stream().<D>map(discoveryRole::deserialize).collect(Collectors.toList());
            refreshDiscoveredServers(servers);

            updateRefresh();
        }, 3, 1000L);
    }

    private boolean requireRefresh() {
        return nextRefreshTimeMillis < System.currentTimeMillis();
    }

    private void updateRefresh() {
        this.nextRefreshTimeMillis = System.currentTimeMillis() + sessionTimeoutMs;
    }

}
