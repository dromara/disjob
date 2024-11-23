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

package cn.ponfee.disjob.registry.zookeeper;

import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.registry.RegistryException;
import cn.ponfee.disjob.registry.ServerRegistry;
import cn.ponfee.disjob.registry.zookeeper.configuration.ZookeeperRegistryProperties;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PreDestroy;
import java.util.List;

/**
 * Registry server based zookeeper.
 *
 * @author Ponfee
 */
public abstract class ZookeeperServerRegistry<R extends Server, D extends Server> extends ServerRegistry<R, D> {

    private static final int CREATE_EPHEMERAL_FAIL_RETRIES = 3;

    private final CuratorFrameworkClient client;
    private final String zkRegistryRootPath;

    protected ZookeeperServerRegistry(ZookeeperRegistryProperties config, RestTemplate restTemplate) {
        super(config, restTemplate, '/');
        // zookeeper parent path must start with "/"
        this.zkRegistryRootPath = separator + registryRootPath;
        String zkDiscoveryRootPath = separator + discoveryRootPath;

        CuratorFrameworkClient client0 = null;
        try {
            this.client = client0 = new CuratorFrameworkClient(config, c -> {
                if (state.isStopped()) {
                    return;
                }
                for (R server : registered) {
                    try {
                        c.createEphemeral(buildRegistryPath(server), CREATE_EPHEMERAL_FAIL_RETRIES);
                    } catch (Throwable t) {
                        log.error("Re-registry server to zookeeper occur error: " + server, t);
                    }
                }
            });
            client.createPersistent(zkRegistryRootPath);
            client.createPersistent(zkDiscoveryRootPath);
            client.watch(zkDiscoveryRootPath, this::refreshDiscoveryServers);
        } catch (Throwable t) {
            if (client0 != null) {
                client0.close();
            }
            throw new RegistryException("Zookeeper registry init error: " + config, t);
        }
    }

    @Override
    public final boolean isConnected() {
        return client.isConnected();
    }

    // ------------------------------------------------------------------Registry

    @Override
    public final void register(R server) {
        if (state.isStopped()) {
            return;
        }

        try {
            client.createEphemeral(buildRegistryPath(server), CREATE_EPHEMERAL_FAIL_RETRIES);
            registered.add(server);
            log.info("Server registered: {}, {}", registryRole, server);
        } catch (Throwable e) {
            throw new RegistryException("Zookeeper server register failed: " + server, e);
        }
    }

    @Override
    public final void deregister(R server) {
        String registryPath = buildRegistryPath(server);
        try {
            registered.remove(server);
            client.deletePath(registryPath);
            log.info("Server deregister: {}, {}", registryRole, server);
        } catch (Throwable e) {
            log.error("Deregister to zookeeper failed: " + registryPath, e);
        }
    }

    @Override
    public List<R> getRegisteredServers() {
        try {
            List<String> servers = client.getChildren(zkRegistryRootPath);
            return deserializeServers(servers, registryRole);
        } catch (Exception e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    // ------------------------------------------------------------------Close

    @PreDestroy
    @Override
    public void close() {
        if (!state.stop()) {
            return;
        }

        registered.forEach(this::deregister);
        ThrowingRunnable.doCaught(client::close);
        super.close();
    }

    // ------------------------------------------------------------------private methods

    private String buildRegistryPath(R server) {
        return zkRegistryRootPath + separator + server.serialize();
    }

}
