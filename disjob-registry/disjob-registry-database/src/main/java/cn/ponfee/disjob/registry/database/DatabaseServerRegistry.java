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

package cn.ponfee.disjob.registry.database;

import cn.ponfee.disjob.common.base.RetryTemplate;
import cn.ponfee.disjob.common.concurrent.LoopThread;
import cn.ponfee.disjob.common.concurrent.Threads;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingSupplier;
import cn.ponfee.disjob.common.spring.JdbcTemplateWrapper;
import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.registry.ServerRegistry;
import cn.ponfee.disjob.registry.database.configuration.DatabaseRegistryProperties;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.Assert;

import javax.annotation.PreDestroy;
import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.List;
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
    private static final String TABLE_NAME = "disjob_registry";

    private static final String CREATE_TABLE_DDL =
        "CREATE TABLE IF NOT EXISTS `" + TABLE_NAME + "` (                                                         \n" +
        "  `id`              BIGINT        UNSIGNED  NOT NULL  AUTO_INCREMENT  COMMENT 'auto increment id',        \n" +
        "  `namespace`       VARCHAR(60)             NOT NULL                  COMMENT 'registry namespace',       \n" +
        "  `role`            VARCHAR(30)             NOT NULL                  COMMENT 'role(worker, supervisor)', \n" +
        "  `server`          VARCHAR(255)            NOT NULL                  COMMENT 'server serialization',     \n" +
        "  `heartbeat_time`  BIGINT        UNSIGNED  NOT NULL                  COMMENT 'last heartbeat time',      \n" +
        "  PRIMARY KEY (`id`),                                                                                     \n" +
        "  UNIQUE KEY `uk_namespace_role_server` (`namespace`, `role`, `server`)                                   \n" +
        ") ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Database registry'; \n" ;

    private static final String REMOVE_DEAD_SQL = "DELETE FROM " + TABLE_NAME + " WHERE namespace=? AND role=? AND heartbeat_time<?";

    private static final String REGISTER_SQL    = "INSERT INTO " + TABLE_NAME + " (namespace, role, server, heartbeat_time) VALUES (?, ?, ?, ?)";

    private static final String HEARTBEAT_SQL   = "UPDATE " + TABLE_NAME + " SET heartbeat_time=? WHERE namespace=? AND role=? AND server=?";

    private static final String DEREGISTER_SQL  = "DELETE FROM " + TABLE_NAME + " WHERE namespace=? AND role=? AND server=?";

    private static final String SELECT_SQL      = "SELECT server FROM " + TABLE_NAME + " WHERE namespace=? AND role=? AND heartbeat_time>?";

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

    // -------------------------------------------------Registry

    private final String registerRoleName;
    private final LoopThread registerHeartbeatThread;

    // -------------------------------------------------Discovery

    private final String discoveryRoleName;
    private final LoopThread discoverHeartbeatThread;

    protected DatabaseServerRegistry(DatabaseRegistryProperties config, JdbcTemplateWrapper wrapper) {
        super(config.getNamespace(), ':');
        this.namespace = config.getNamespace().trim();
        this.jdbcTemplateWrapper = wrapper;
        this.sessionTimeoutMs = config.getSessionTimeoutMs();

        long periodMs = config.getSessionTimeoutMs() / 3;

        // -------------------------------------------------registry
        this.registerRoleName = registryRole.name().toLowerCase();

        // create table
        jdbcTemplateWrapper.createTableIfNotExists(TABLE_NAME, CREATE_TABLE_DDL);

        // remove dead server
        Object[] args = {namespace, registerRoleName, System.currentTimeMillis() - DEAD_TIME_MILLIS};
        RetryTemplate.executeQuietly(() -> jdbcTemplateWrapper.delete(REMOVE_DEAD_SQL, args), 3, 1000L);

        // heartbeat register servers
        this.registerHeartbeatThread = LoopThread.createStarted("database_register_heartbeat", periodMs, periodMs, this::registerServers);

        // -------------------------------------------------discovery
        this.discoveryRoleName = discoveryRole.name().toLowerCase();

        // heartbeat discover servers
        this.discoverHeartbeatThread = LoopThread.createStarted("database_discover_heartbeat", periodMs, periodMs, this::discoverServers);

        // initialize discover server
        try {
            discoverServers();
        } catch (Throwable e) {
            Threads.interruptIfNecessary(e);
            close();
            throw new Error("Database init discover error.", e);
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

        jdbcTemplateWrapper.executeInTransaction(psCreator -> {
            String serialize = server.serialize();
            PreparedStatement update = psCreator.apply(HEARTBEAT_SQL);
            update.setLong(1, System.currentTimeMillis());
            update.setString(2, namespace);
            update.setString(3, registerRoleName);
            update.setString(4, serialize);
            int updateRowsAffected = update.executeUpdate();
            Assert.isTrue(updateRowsAffected <= AFFECTED_ONE_ROW, () -> "Invalid update rows affected: " + updateRowsAffected);
            if (updateRowsAffected == AFFECTED_ONE_ROW) {
                log.info("Database register update: {}, {}, {}", namespace, registerRoleName, serialize);
            } else {
                PreparedStatement insert = psCreator.apply(REGISTER_SQL);
                insert.setString(1, namespace);
                insert.setString(2, registerRoleName);
                insert.setString(3, serialize);
                insert.setLong(4, System.currentTimeMillis());
                int insertRowsAffected = insert.executeUpdate();
                Assert.isTrue(insertRowsAffected == AFFECTED_ONE_ROW, () -> "Invalid insert rows affected: " + insertRowsAffected);
                log.info("Database register insert: {}, {}, {}", namespace, insertRowsAffected, serialize);
            }
        });

        registered.add(server);
    }

    @Override
    public final void deregister(R server) {
        registered.remove(server);
        Object[] args = new Object[]{namespace, registerRoleName, server.serialize()};
        ThrowingSupplier.doCaught(() -> jdbcTemplateWrapper.delete(DEREGISTER_SQL, args));
        log.info("Server deregister: {}, {}", registryRole, server);
    }

    @Override
    public List<R> getRegisteredServers() {
        Object[] args = {namespace, registerRoleName, System.currentTimeMillis() - sessionTimeoutMs};
        return deserializeRegistryServers(jdbcTemplateWrapper.list(SELECT_SQL, JdbcTemplateWrapper.STRING_ROW_MAPPER, args));
    }

    // ------------------------------------------------------------------Close

    @PreDestroy
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        registerHeartbeatThread.terminate();
        registered.forEach(this::deregister);
        registered.clear();
        discoverHeartbeatThread.terminate();
        super.close();
    }

    // ------------------------------------------------------------------private methods

    /**
     * 心跳注册，不需要事务及原子性保证
     */
    private void registerServers() {
        for (R server : registered) {
            final String serialize = server.serialize();
            RetryTemplate.executeQuietly(() -> {
                Object[] updateArgs = {System.currentTimeMillis(), namespace, registerRoleName, serialize};
                if (jdbcTemplateWrapper.update(HEARTBEAT_SQL, updateArgs) == AFFECTED_ONE_ROW) {
                    log.debug("Database heartbeat register update: {}, {}, {}, {}", updateArgs);
                    return;
                }

                Object[] insertArgs = {namespace, registerRoleName, serialize, System.currentTimeMillis()};
                jdbcTemplateWrapper.insert(REGISTER_SQL, insertArgs);
                log.debug("Database heartbeat register insert: {}, {}, {}, {}", insertArgs);
            }, 3, 1000L);
        }
    }

    private void discoverServers() throws Throwable {
        RetryTemplate.execute(() -> {
            Object[] args = {namespace, discoveryRoleName, System.currentTimeMillis() - sessionTimeoutMs};
            List<String> discovered = jdbcTemplateWrapper.list(SELECT_SQL, JdbcTemplateWrapper.STRING_ROW_MAPPER, args);

            if (CollectionUtils.isEmpty(discovered)) {
                log.warn("Not discovered available {} from database.", discoveryRole);
                discovered = Collections.emptyList();
            }

            List<D> servers = discovered.stream().<D>map(discoveryRole::deserialize).collect(Collectors.toList());
            refreshDiscoveredServers(servers);

            log.debug("Database discovered {} servers.", discoveryRole);
        }, 3, 1000L);
    }

}
