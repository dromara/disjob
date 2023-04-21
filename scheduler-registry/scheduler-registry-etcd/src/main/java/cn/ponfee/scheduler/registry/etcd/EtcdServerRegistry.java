/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.registry.etcd;

import cn.ponfee.scheduler.common.base.Symbol.Char;
import cn.ponfee.scheduler.common.concurrent.NamedThreadFactory;
import cn.ponfee.scheduler.common.exception.Throwables;
import cn.ponfee.scheduler.common.util.ObjectUtils;
import cn.ponfee.scheduler.core.base.Server;
import cn.ponfee.scheduler.registry.ConnectionStateListener;
import cn.ponfee.scheduler.registry.ServerRegistry;
import cn.ponfee.scheduler.registry.etcd.configuration.EtcdRegistryProperties;
import io.etcd.jetcd.common.exception.ErrorCode;
import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.support.CloseableClient;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
     * check keep alive available
     */
    private final ScheduledExecutorService keepAliveCheckScheduler;

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
            this.keepAliveCheckScheduler = new ScheduledThreadPoolExecutor(
                1, NamedThreadFactory.builder().prefix("keep_alive_check_scheduler").daemon(true).build()
            );

            client.createPersistentKey(registryRootPath, PLACEHOLDER_VALUE);
            createLeaseIdAndKeepAlive();
            client.watchChildChanged(discoveryRootPath, latch, this::doRefreshDiscoveryServers);

            long period = Math.max(ttl / 4, 1);
            keepAliveCheckScheduler.scheduleWithFixedDelay(this::keepAliveCheck, period, period, TimeUnit.SECONDS);

            client.addConnectionStateListener(
                ConnectionStateListener.<EtcdClient>builder().onConnected(c -> keepAliveRecover()).build()
            );

            doRefreshDiscoveryServers(client.getKeyChildren(discoveryRootPath));
        } catch (Exception e) {
            throw new IllegalStateException(e);
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
            throw new RuntimeException("Etcd server registered failed: " + server, e);
        }
    }

    @Override
    public final void deregister(R server) {
        try {
            registered.remove(server);
            client.deleteKey(buildRegistryServerId(server));
            log.info("Etcd server deregister: {} | {}", registryRole.name(), server);
        } catch (Exception e) {
            log.error("Etcd server deregister error.", e);
        }
    }

    private String buildRegistryServerId(R server) {
        return registryRootPath + separator + server.serialize();
    }

    // ------------------------------------------------------------------Close

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            log.warn("Repeat call close method\n{}", ObjectUtils.getStackTrace());
            return;
        }

        Throwables.caught(keepAliveCheckScheduler::shutdownNow);
        Throwables.caught(() -> keepAliveCheckScheduler.awaitTermination(1, TimeUnit.SECONDS));
        Throwables.caught(keepAlive::close);
        registered.forEach(this::deregister);
        registered.clear();
        Throwables.caught(() -> client.revokeLease(leaseId));
        Throwables.caught(client::close);
        Throwables.caught(super::close);
    }

    // ------------------------------------------------------------------private method

    private synchronized void doRefreshDiscoveryServers(List<String> list) {
        List<D> servers;
        if (CollectionUtils.isEmpty(list)) {
            log.error("Not discovered available {} from etcd.", discoveryRole.name());
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
                } catch (Exception e) {
                    log.error("keep alive check occur error.", e);
                }
            }
        }
    }

    private void keepAliveRecover() {
        synchronized (keepAliveLock) {
            try {
                if (this.keepAlive != null) {
                    this.keepAlive.close();
                    this.keepAlive = null;
                    client.revokeLease(leaseId);
                }
                createLeaseIdAndKeepAlive();
            } catch (Exception e) {
                log.error("Keep alive retry occur error.", e);
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
