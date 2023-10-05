/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry.zookeeper;

import cn.ponfee.disjob.common.base.Symbol.Char;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.util.ObjectUtils;
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

    protected ZookeeperServerRegistry(ZookeeperRegistryProperties config) {
        super(config.getNamespace(), Char.SLASH);
        // zookeeper parent path must start with "/"
        String registryRootPath0 = separator + registryRootPath;
        String discoveryRootPath0 = separator + discoveryRootPath;

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
            client.createPersistent(registryRootPath0);
            client.createPersistent(discoveryRootPath0);
            //client.listenChildChanged(discoveryRootPath0);
            client.watchChildChanged(discoveryRootPath0, latch, this::doRefreshDiscoveryServers);
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
            log.info("Server registered: {} | {}", registryRole.name(), server);
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
            log.info("Server deregister: {} | {}", registryRole.name(), server);
        } catch (Throwable e) {
            log.error("Deregister to zookeeper failed: " + registryPath, e);
        }
    }

    // ------------------------------------------------------------------Close

    @PreDestroy
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        registered.forEach(this::deregister);
        ThrowingRunnable.execute(client::close);
        registered.clear();
        super.close();
    }

    // ------------------------------------------------------------------private methods

    private String buildRegistryPath(R server) {
        return separator + registryRootPath + separator + server.serialize();
    }

    private synchronized void doRefreshDiscoveryServers(List<String> list) {
        List<D> servers;
        log.info("Watched servers {}", list);
        if (CollectionUtils.isEmpty(list)) {
            log.warn("Not discovered available {} from zookeeper.", discoveryRole.name());
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
