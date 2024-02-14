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

import cn.ponfee.disjob.common.base.Symbol.Char;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.registry.RegistryException;
import cn.ponfee.disjob.registry.ServerRegistry;
import cn.ponfee.disjob.registry.zookeeper.configuration.ZookeeperRegistryProperties;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * Registry server based zookeeper.
 *
 * @author Ponfee
 */
public abstract class ZookeeperServerRegistry<R extends Server, D extends Server> extends ServerRegistry<R, D> {

    private static final int CREATE_EPHEMERAL_FAIL_RETRIES = 3;

    private final CuratorFrameworkClient client;
    private final String zkRegistryRootPath;

    protected ZookeeperServerRegistry(ZookeeperRegistryProperties config) {
        super(config.getNamespace(), Char.SLASH);
        // zookeeper parent path must start with "/"
        this.zkRegistryRootPath = separator + registryRootPath;
        String zkDiscoveryRootPath = separator + discoveryRootPath;

        CountDownLatch latch = new CountDownLatch(1);
        try {
            this.client = new CuratorFrameworkClient(config, client0 -> {
                if (closed.get()) {
                    return;
                }
                for (R server : registered) {
                    try {
                        client0.createEphemeral(buildRegistryPath(server), CREATE_EPHEMERAL_FAIL_RETRIES);
                    } catch (Throwable t) {
                        log.error("Re-registry server to zookeeper occur error: " + server, t);
                    }
                }
            });
            client.createPersistent(zkRegistryRootPath);
            client.createPersistent(zkDiscoveryRootPath);
            //client.listenChildChanged(zkDiscoveryRootPath);
            client.watchChildChanged(zkDiscoveryRootPath, latch, this::doRefreshDiscoveryServers);
        } catch (Exception e) {
            throw new RegistryException("Zookeeper registry init error: " + config, e);
        } finally {
            latch.countDown();
        }
    }

    @Override
    public final boolean isConnected() {
        return client.isConnected();
    }

    // ------------------------------------------------------------------Registry

    @Override
    public final void register(R server) {
        if (closed.get()) {
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
    public List<R> getRegisteredServers() throws Exception {
        return deserializeRegistryServers(client.getChildren(zkRegistryRootPath));
    }

    // ------------------------------------------------------------------Close

    @PreDestroy
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        registered.forEach(this::deregister);
        ThrowingRunnable.doCaught(client::close);
        registered.clear();
        super.close();
    }

    // ------------------------------------------------------------------private methods

    private String buildRegistryPath(R server) {
        return zkRegistryRootPath + separator + server.serialize();
    }

    private synchronized void doRefreshDiscoveryServers(List<String> list) {
        List<D> servers;
        log.info("Watched servers {}", list);
        if (CollectionUtils.isEmpty(list)) {
            log.warn("Not discovered available {} from zookeeper.", discoveryRole);
            servers = Collections.emptyList();
        } else {
            servers = list.stream()
                .filter(Objects::nonNull)
                .<D>map(discoveryRole::deserialize)
                .collect(Collectors.toList());
        }
        refreshDiscoveredServers(servers);
    }

}
