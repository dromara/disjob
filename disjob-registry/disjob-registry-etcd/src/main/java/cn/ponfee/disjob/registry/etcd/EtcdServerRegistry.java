/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry.etcd;

import cn.ponfee.disjob.common.base.LoopThread;
import cn.ponfee.disjob.common.base.Symbol.Char;
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
import java.util.concurrent.CountDownLatch;
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
        super(config.getNamespace(), Char.SLASH);
        this.ttl = config.getSessionTimeoutMs() / 2000;

        CountDownLatch latch = new CountDownLatch(1);
        try {
            this.client = new EtcdClient(config);

            client.createPersistentKey(registryRootPath, PLACEHOLDER_VALUE);
            createLeaseIdAndKeepAlive();
            client.watchChildChanged(discoveryRootPath, latch, this::doRefreshDiscoveryServers);

            long periodMs = Math.max(ttl / 4, 1) * 1000;
            this.keepAliveCheckThread = LoopThread.createStarted("etcd_keep_alive_check", periodMs, periodMs, this::keepAliveCheck);

            client.addConnectionStateListener(
                ConnectionStateListener.<EtcdClient>builder().onConnected(c -> keepAliveRecover()).build()
            );

            doRefreshDiscoveryServers(client.getKeyChildren(discoveryRootPath));
        } catch (Exception e) {
            throw new RegistryException("Etcd registry init error: " + config, e);
        } finally {
            latch.countDown();
        }
    }

    // ------------------------------------------------------------------Registry

    @Override
    public final boolean isConnected() {
        return client.isConnected();
    }

    @Override
    public final void register(R server) {
        if (closed.get()) {
            return;
        }

        try {
            client.createEphemeralKey(buildRegistryServerId(server), PLACEHOLDER_VALUE, leaseId);
            registered.add(server);
            log.info("Etcd server registered: {} | {}", registryRole.name(), server);
        } catch (Throwable e) {
            throw new RegistryException("Etcd server register failed: " + server, e);
        }
    }

    @Override
    public final void deregister(R server) {
        try {
            registered.remove(server);
            client.deleteKey(buildRegistryServerId(server));
            log.info("Etcd server deregister: {} | {}", registryRole.name(), server);
        } catch (Throwable t) {
            log.error("Etcd server deregister error.", t);
        }
    }

    private String buildRegistryServerId(R server) {
        return registryRootPath + separator + server.serialize();
    }

    // ------------------------------------------------------------------Close

    @PreDestroy
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        keepAliveCheckThread.terminate();
        registered.forEach(this::deregister);
        registered.clear();
        ThrowingRunnable.execute(keepAlive::close);
        ThrowingRunnable.execute(() -> client.revokeLease(leaseId));
        ThrowingRunnable.execute(client::close);
        ThrowingRunnable.execute(super::close);
    }

    // ------------------------------------------------------------------private method

    private synchronized void doRefreshDiscoveryServers(List<String> list) {
        List<D> servers;
        if (CollectionUtils.isEmpty(list)) {
            log.warn("Not discovered available {} from etcd.", discoveryRole.name());
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
            try {
                if (this.keepAlive != null) {
                    ThrowingRunnable.execute(this.keepAlive::close);
                    this.keepAlive = null;
                    ThrowingRunnable.execute(() -> client.revokeLease(leaseId));
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
