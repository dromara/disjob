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
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.util.Assert;

import javax.annotation.PreDestroy;
import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
        "  UNIQUE KEY `uk_namespace_role_server` (`namespace`, `role`, `server`)                                              \n" +
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

    /**
     * Session timeout milliseconds
     */
    private final long sessionTimeoutMs;

    /**
     * Register period milliseconds
     */
    private final long registryPeriodMs;

    // -------------------------------------------------Registry

    private final String registerRoleName;
    private final LoopProcessThread registerHeartbeatThread;

    // -------------------------------------------------Discovery

    private final String discoveryRoleName;
    private final ThreadPoolExecutor asyncRefreshExecutor;
    private final Lock asyncRefreshLock = new ReentrantLock();
    private volatile long nextRefreshTimeMillis = 0;

    protected DatabaseServerRegistry(DatabaseRegistryProperties config, JdbcTemplateWrapper wrapper) {
        super(config.getNamespace(), ':');
        this.namespace = config.getNamespace().trim();
        this.jdbcTemplateWrapper = wrapper;
        this.sessionTimeoutMs = config.getSessionTimeoutMs();
        this.registryPeriodMs = config.getSessionTimeoutMs() / 3;

        // ---------------------------registry---------------------------
        this.registerRoleName = registryRole.name().toLowerCase();

        // create table
        try {
            RetryTemplate.execute(() -> jdbcTemplateWrapper.createTableIfNotExists(TABLE_NAME, CREATE_TABLE_DDL), 3, 1000L);
        } catch (Throwable e) {
            Threads.interruptIfNecessary(e);
            throw new Error("Create " + TABLE_NAME + " table failed.", e);
        }

        // remove dead server
        Object[] args = {namespace, registerRoleName, System.currentTimeMillis() - DEAD_TIME_MILLIS};
        RetryTemplate.executeQuietly(() -> jdbcTemplateWrapper.delete(REMOVE_DEAD_SQL, args), 3, 1000L);

        // heartbeat register server
        this.registerHeartbeatThread = new LoopProcessThread(
            "database_register_heartbeat", registryPeriodMs, registryPeriodMs, this::doHeartbeatRegister
        );
        registerHeartbeatThread.start();

        // ---------------------------discovery---------------------------
        this.discoveryRoleName = discoveryRole.name().toLowerCase();
        this.asyncRefreshExecutor = ThreadPoolExecutors.builder()
            .corePoolSize(1)
            .maximumPoolSize(1)
            .workQueue(new SynchronousQueue<>())
            .keepAliveTimeSeconds(300)
            .rejectedHandler(ThreadPoolExecutors.DISCARD)
            .threadFactory(NamedThreadFactory.builder().prefix("database_async_discovery").priority(Thread.MAX_PRIORITY).build())
            .build();

        // initialize discovery server
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
        try {
            jdbcTemplateWrapper.existsTable(TABLE_NAME);
            return true;
        } catch (Throwable t) {
            Threads.interruptIfNecessary(t);
            return false;
        }
    }

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

    /**
     * Server注册，需要加事务保持原子性
     *
     * @param server the registering server
     */
    @Override
    public final void register(R server) {
        if (closed.get()) {
            return;
        }

        final Set<R> servers = splitServer(server);
        jdbcTemplateWrapper.executeInTransaction(action -> {
            for (R svr : servers) {
                String serialize = svr.serialize();
                PreparedStatement update = action.apply(HEARTBEAT_SQL);
                update.setLong(1, System.currentTimeMillis());
                update.setString(2, namespace);
                update.setString(3, registerRoleName);
                update.setString(4, serialize);
                int updateRowsAffected = update.executeUpdate();
                Assert.isTrue(updateRowsAffected <= AFFECTED_ONE_ROW, () -> "Invalid insert rows affected: " + updateRowsAffected);
                if (updateRowsAffected == AFFECTED_ONE_ROW) {
                    log.info("Database register update: {} | {} | {}", namespace, registerRoleName, serialize);
                    continue;
                }

                PreparedStatement insert = action.apply(REGISTER_SQL);
                insert.setString(1, namespace);
                insert.setString(2, registerRoleName);
                insert.setString(3, serialize);
                insert.setLong(4, System.currentTimeMillis());
                int insertRowsAffected = insert.executeUpdate();
                Assert.isTrue(insertRowsAffected == AFFECTED_ONE_ROW, () -> "Invalid insert rows affected: " + insertRowsAffected);
                log.info("Database register insert: {} | {} | {}", namespace, insertRowsAffected, serialize);
            }

            return null;
        });

        registered.addAll(servers);
    }

    @Override
    public final void deregister(R server) {
        Set<R> servers = splitServer(server);
        for (R svr : servers) {
            registered.remove(svr);
            Object[] args = new Object[]{namespace, registerRoleName, svr.serialize()};
            ThrowingSupplier.execute(() -> jdbcTemplateWrapper.delete(DEREGISTER_SQL, args));
            log.info("Server deregister: {} | {}", registryRole.name(), svr);
        }
    }

    // ------------------------------------------------------------------Close

    @PreDestroy
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            log.warn("Repeat call close method\n{}", ObjectUtils.getStackTrace());
            return;
        }

        registerHeartbeatThread.terminate();
        registered.forEach(this::deregister);
        registered.clear();
        ThrowingSupplier.execute(() -> ThreadPoolExecutors.shutdown(asyncRefreshExecutor, 1));
        ThrowingRunnable.execute(jdbcTemplateWrapper::close);
        super.close();
    }

    // ------------------------------------------------------------------private methods

    private Set<R> splitServer(R server) {
        if (!(server instanceof Worker)) {
            return Collections.singleton(server);
        }

        Set<R> servers = new HashSet<>();
        for (Worker worker : ((Worker) server).splitGroup()) {
            servers.add((R) worker);
        }
        return servers;
    }

    /**
     * 心跳注册，不需要原子性
     */
    private void doHeartbeatRegister() {
        for (R server : registered) {
            final String serialize = server.serialize();
            RetryTemplate.executeQuietly(() -> {
                Object[] updateArgs = {System.currentTimeMillis(), namespace, registerRoleName, serialize};
                if (jdbcTemplateWrapper.update(HEARTBEAT_SQL, updateArgs) == AFFECTED_ONE_ROW) {
                    log.debug("Database heartbeat register update: {} | {} | {} | {}", updateArgs);
                    return;
                }

                Object[] insertArgs = {namespace, registerRoleName, serialize, System.currentTimeMillis()};
                jdbcTemplateWrapper.insert(REGISTER_SQL, insertArgs);
                log.debug("Database heartbeat register insert: {} | {} | {} | {}", insertArgs);
            }, 3, 1000L);
        }
    }

    private void asyncRefreshDiscoveryServers() {
        if (!requireRefresh()) {
            return;
        }
        asyncRefreshExecutor.execute(() -> {
            if (!requireRefresh()) {
                return;
            }
            if (!asyncRefreshLock.tryLock()) {
                return;
            }
            try {
                doRefreshDiscoveryServers();
            } catch (Throwable t) {
                Threads.interruptIfNecessary(t);
                log.error("Database async refresh discovery servers occur error.", t);
            } finally {
                asyncRefreshLock.unlock();
            }
        });
    }

    private void doRefreshDiscoveryServers() throws Throwable {
        RetryTemplate.execute(() -> {
            Object[] args = {namespace, discoveryRoleName, System.currentTimeMillis() - sessionTimeoutMs};
            List<String> discovered = jdbcTemplateWrapper.query(DISCOVER_SQL, ROW_MAPPER, args);

            if (CollectionUtils.isEmpty(discovered)) {
                log.warn("Not discovered available {} from database.", discoveryRole.name());
                discovered = Collections.emptyList();
            }

            List<D> servers = discovered.stream().<D>map(discoveryRole::deserialize).collect(Collectors.toList());
            refreshDiscoveredServers(servers);

            updateRefresh();
            log.debug("Database refreshed discovery {} servers.", discoveryRole.name());
        }, 3, 1000L);
    }

    private boolean requireRefresh() {
        return !closed.get() && nextRefreshTimeMillis < System.currentTimeMillis();
    }

    private void updateRefresh() {
        this.nextRefreshTimeMillis = System.currentTimeMillis() + registryPeriodMs;
    }

}
