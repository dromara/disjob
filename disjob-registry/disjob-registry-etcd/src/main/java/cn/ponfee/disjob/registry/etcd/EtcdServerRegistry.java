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

package cn.ponfee.disjob.registry.etcd;

import cn.ponfee.disjob.common.concurrent.LoopThread;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.registry.ConnectionStateListener;
import cn.ponfee.disjob.registry.RegistryException;
import cn.ponfee.disjob.registry.ServerRegistry;
import cn.ponfee.disjob.registry.etcd.configuration.EtcdRegistryProperties;
import io.etcd.jetcd.common.exception.ErrorCode;
import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.support.CloseableClient;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Registry server based etcd.
 *
 * @author Ponfee
 */
public abstract class EtcdServerRegistry<R extends Server, D extends Server> extends ServerRegistry<R, D> {

    private static final String PLACEHOLDER_VALUE = "1";

    private final Object keepAliveLock = new Object();

    /**
     * Etcd lease ttl(seconds)
     */
    private final long ttl;

    /**
     * Etcd client
     */
    private final EtcdClient client;

    /**
     * Keep alive check thread
     */
    private final LoopThread keepAliveCheckThread;

    /**
     * Etcd lease id
     */
    private volatile long leaseId;

    /**
     * Keep alive holder
     */
    private volatile CloseableClient keepAlive;

    protected EtcdServerRegistry(EtcdRegistryProperties config) {
        // etcd separator must be '/'
        super(config, '/');
        this.ttl = config.getSessionTimeoutMs() / 2000;

        EtcdClient client0 = null;
        try {
            this.client = client0 = new EtcdClient(config);

            client.createPersistentKey(registryRootPath, PLACEHOLDER_VALUE);
            createLeaseIdAndKeepAlive();
            client.watch(discoveryRootPath, this::doRefreshDiscoveryServers);

            long periodMs = Math.max(ttl / 4, 1) * 1000;
            this.keepAliveCheckThread = LoopThread.createStarted("etcd_keep_alive_check", periodMs, periodMs, this::keepAliveCheck);

            client.addConnectionStateListener(
                ConnectionStateListener.<EtcdClient>builder().onConnected(c -> keepAliveRecover()).build()
            );

            doRefreshDiscoveryServers(client.getKeyChildren(discoveryRootPath));
        } catch (Throwable t) {
            if (client0 != null) {
                client0.close();
            }
            throw new RegistryException("Etcd registry init error: " + config, t);
        }
    }

    // ------------------------------------------------------------------Registry

    @Override
    public final boolean isConnected() {
        return client.isConnected();
    }

    @Override
    public final void register(R server) {
        if (state.isStopped()) {
            return;
        }

        try {
            client.createEphemeralKey(buildRegistryServerId(server), PLACEHOLDER_VALUE, leaseId);
            registered.add(server);
            log.info("Etcd server registered: {}, {}", registryRole, server);
        } catch (Throwable e) {
            throw new RegistryException("Etcd server register failed: " + server, e);
        }
    }

    @Override
    public final void deregister(R server) {
        try {
            registered.remove(server);
            client.deleteKey(buildRegistryServerId(server));
            log.info("Etcd server deregister: {}, {}", registryRole, server);
        } catch (Throwable t) {
            log.error("Etcd server deregister error.", t);
        }
    }

    @Override
    public List<R> getRegisteredServers() throws Exception {
        return deserializeRegistryServers(client.getKeyChildren(registryRootPath));
    }

    // ------------------------------------------------------------------Close

    @PreDestroy
    @Override
    public void close() {
        if (!state.stop()) {
            return;
        }

        keepAliveCheckThread.terminate();
        registered.forEach(this::deregister);
        final CloseableClient keepAlive0 = this.keepAlive;
        if (keepAlive0 != null) {
            ThrowingRunnable.doCaught(keepAlive0::close);
        }
        ThrowingRunnable.doCaught(() -> client.revokeLease(leaseId));
        ThrowingRunnable.doCaught(client::close);
        super.close();
    }

    // ------------------------------------------------------------------private method

    private String buildRegistryServerId(R server) {
        return registryRootPath + separator + server.serialize();
    }

    private synchronized void doRefreshDiscoveryServers(List<String> list) {
        List<D> servers;
        if (CollectionUtils.isEmpty(list)) {
            log.warn("Not discovered available {} from etcd.", discoveryRole);
            servers = Collections.emptyList();
        } else {
            servers = list.stream()
                .filter(Objects::nonNull)
                .<D>map(discoveryRole::deserialize)
                .collect(Collectors.toList());
        }
        refreshDiscoveredServers(servers);
    }

    private void keepAliveCheck() {
        synchronized (keepAliveLock) {
            if (keepAlive == null) {
                log.warn("Keep alive is null, will be create.");
                try {
                    createLeaseIdAndKeepAlive();
                } catch (Throwable t) {
                    log.error("keep alive check occur error.", t);
                }
            }
        }
    }

    private void keepAliveRecover() {
        synchronized (keepAliveLock) {
            final CloseableClient keepAlive0 = this.keepAlive;
            try {
                if (keepAlive0 != null) {
                    ThrowingRunnable.doCaught(keepAlive0::close);
                    this.keepAlive = null;
                    ThrowingRunnable.doCaught(() -> client.revokeLease(leaseId));
                }
                createLeaseIdAndKeepAlive();
            } catch (Throwable t) {
                log.error("Keep alive retry occur error.", t);
            }
        }
    }

    private void createLeaseIdAndKeepAlive() throws Exception {
        this.leaseId = client.createLease(ttl);
        this.keepAlive = client.keepAliveLease(
            leaseId,
            t -> {
                if (t instanceof EtcdException) {
                    EtcdException e = (EtcdException) t;
                    log.error("Keep alive on error: " + e.getErrorCode(), t);
                    if (e.getErrorCode() != ErrorCode.NOT_FOUND) {
                        // ttl has expired
                        keepAliveRecover();
                    }
                } else {
                    log.error("Keep alive on fail.", t);
                }
            },
            () -> {
                // deadline reached
                log.error("Keep alive on completed.");
                keepAliveRecover();
            }
        );
        registered.forEach(this::register);
    }

}
