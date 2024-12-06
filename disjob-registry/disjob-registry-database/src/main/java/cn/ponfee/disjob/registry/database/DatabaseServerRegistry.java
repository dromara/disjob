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
import cn.ponfee.disjob.common.spring.JdbcTemplateWrapper;
import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.core.enums.RegistryEventType;
import cn.ponfee.disjob.registry.ServerRegistry;
import cn.ponfee.disjob.registry.database.configuration.DatabaseRegistryProperties;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PreDestroy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static cn.ponfee.disjob.common.spring.TransactionUtils.assertOneAffectedRow;
import static cn.ponfee.disjob.common.spring.TransactionUtils.hasAffectedRow;

/**
 * Registry server based database.
 *
 * @author Ponfee
 */
public abstract class DatabaseServerRegistry<R extends Server, D extends Server> extends ServerRegistry<R, D> {

    private static final long DEAD_TIME_MILLIS = TimeUnit.HOURS.toMillis(12);
    private static final String TABLE_NAME = "sched_registry";

    private static final String CREATE_TABLE_DDL =
        "CREATE TABLE IF NOT EXISTS `" + TABLE_NAME + "` (                                                                      \n" +
        "  `id`              BIGINT        UNSIGNED  NOT NULL  AUTO_INCREMENT  COMMENT 'auto increment primary key id',         \n" +
        "  `namespace`       VARCHAR(60)             NOT NULL                  COMMENT 'registry namespace',                    \n" +
        "  `role`            VARCHAR(30)             NOT NULL                  COMMENT 'role(worker, supervisor)',              \n" +
        "  `server`          VARCHAR(255)            NOT NULL                  COMMENT 'server serialization',                  \n" +
        "  `heartbeat_time`  BIGINT        UNSIGNED  NOT NULL                  COMMENT 'last heartbeat time',                   \n" +
        "  PRIMARY KEY (`id`),                                                                                                  \n" +
        "  UNIQUE KEY `uk_namespace_role_server` (`namespace`, `role`, `server`)                                                \n" +
        ") ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Disjob registry based database'; \n" ;

    private static final String REMOVE_DEAD_SQL = "DELETE FROM " + TABLE_NAME + " WHERE namespace=? AND role=? AND heartbeat_time<?";

    private static final String REGISTER_SQL    = "INSERT INTO " + TABLE_NAME + " (namespace, role, server, heartbeat_time) VALUES (?, ?, ?, ?)";

    private static final String HEARTBEAT_SQL   = "UPDATE " + TABLE_NAME + " SET heartbeat_time=? WHERE namespace=? AND role=? AND server=?";

    private static final String DEREGISTER_SQL  = "DELETE FROM " + TABLE_NAME + " WHERE namespace=? AND role=? AND server=?";

    private static final String SELECT_SQL      = "SELECT server FROM " + TABLE_NAME + " WHERE namespace=? AND role=? AND heartbeat_time>?";

    private static final String EXISTS_SQL      = "SELECT 1 FROM " + TABLE_NAME + " WHERE namespace=? AND role=? AND server=?";

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

    protected DatabaseServerRegistry(DatabaseRegistryProperties config, RestTemplate restTemplate, JdbcTemplateWrapper jdbcTemplateWrapper) {
        super(config, restTemplate, ':');
        this.namespace = config.getNamespace().trim();
        this.jdbcTemplateWrapper = jdbcTemplateWrapper;
        this.sessionTimeoutMs = config.getSessionTimeoutMs();

        // -------------------------------------------------registry
        this.registerRoleName = registryRole.name().toLowerCase();

        // create table
        jdbcTemplateWrapper.createTableIfNotExists(TABLE_NAME, CREATE_TABLE_DDL);

        // remove dead server
        Object[] args = {namespace, registerRoleName, System.currentTimeMillis() - DEAD_TIME_MILLIS};
        RetryTemplate.executeQuietly(() -> jdbcTemplateWrapper.delete(REMOVE_DEAD_SQL, args), 3, 1000L);

        long periodMs = sessionTimeoutMs / 3;

        // heartbeat register servers
        this.registerHeartbeatThread = LoopThread.createStarted("database_register_heartbeat", periodMs, periodMs, this::registerServers);

        // -------------------------------------------------discovery
        this.discoveryRoleName = discoveryRole.name().toLowerCase();

        // heartbeat discover servers
        this.discoverHeartbeatThread = LoopThread.createStarted("database_discover_heartbeat", periodMs, periodMs, this::discoverServers);
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
        if (state.isStopped()) {
            return;
        }
        register(server.serialize());
        registered.add(server);
        publishServerChanged(RegistryEventType.REGISTER, server);
    }

    @Override
    public final void deregister(R server) {
        registered.remove(server);
        Object[] args = new Object[]{namespace, registerRoleName, server.serialize()};
        RetryTemplate.executeQuietly(() -> jdbcTemplateWrapper.delete(DEREGISTER_SQL, args), 3, 1000L);
        publishServerChanged(RegistryEventType.DEREGISTER, server);
        log.info("Server deregister: {}, {}", registryRole, server);
    }

    @Override
    public List<R> getRegisteredServers() {
        return deserializeServers(getServers(registerRoleName), registryRole);
    }

    // ------------------------------------------------------------------Discovery

    @Override
    public void discoverServers() throws Throwable {
        RetryTemplate.execute(() -> refreshDiscoveryServers(getServers(discoveryRoleName)), 3, 1000L);
    }

    // ------------------------------------------------------------------Close

    @PreDestroy
    @Override
    public void close() {
        if (!state.stop()) {
            return;
        }

        registerHeartbeatThread.terminate();
        registered.forEach(this::deregister);
        discoverHeartbeatThread.terminate();
        super.close();
    }

    // ------------------------------------------------------------------private methods

    /**
     * 心跳注册
     */
    private void registerServers() {
        for (R s : registered) {
            String server = s.serialize();
            RetryTemplate.executeQuietly(() -> register(server), 3, 1000L);
        }
    }

    private void register(String server) {
        jdbcTemplateWrapper.executeInTransaction(psCreator -> {
            // 1、update server heartbeat_time if registered
            PreparedStatement update = psCreator.apply(HEARTBEAT_SQL);
            update.setLong(1, System.currentTimeMillis());
            update.setString(2, namespace);
            update.setString(3, registerRoleName);
            update.setString(4, server);
            int updateAffectedRows = update.executeUpdate();
            if (hasAffectedRow(updateAffectedRows)) {
                assertOneAffectedRow(updateAffectedRows, () -> "Invalid update affected rows: " + updateAffectedRows);
                log.info("Database register update: {}, {}, {}", namespace, registerRoleName, server);
                return;
            }

            // 2、if registered and same heartbeat_time
            PreparedStatement exists = psCreator.apply(EXISTS_SQL);
            exists.setString(1, namespace);
            exists.setString(2, registerRoleName);
            exists.setString(3, server);
            ResultSet rs = exists.executeQuery();
            if (rs.next() && rs.getInt(1) == 1) {
                log.info("Database register exists: {}, {}, {}", namespace, registerRoleName, server);
                return;
            }

            // 3、insert server if unregistered
            PreparedStatement insert = psCreator.apply(REGISTER_SQL);
            insert.setString(1, namespace);
            insert.setString(2, registerRoleName);
            insert.setString(3, server);
            insert.setLong(4, System.currentTimeMillis());
            int insertAffectedRows = insert.executeUpdate();
            assertOneAffectedRow(insertAffectedRows, () -> "Invalid insert affected rows: " + insertAffectedRows);
            log.info("Database register insert: {}, {}, {}", namespace, registerRoleName, server);
        });
    }

    private List<String> getServers(String roleName) {
        Object[] args = {namespace, roleName, System.currentTimeMillis() - sessionTimeoutMs};
        return jdbcTemplateWrapper.list(SELECT_SQL, JdbcTemplateWrapper.STRING_ROW_MAPPER, args);
    }

}
