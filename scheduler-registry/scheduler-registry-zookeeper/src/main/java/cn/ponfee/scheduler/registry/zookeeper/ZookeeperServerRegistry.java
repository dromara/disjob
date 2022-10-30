package cn.ponfee.scheduler.registry.zookeeper;

import cn.ponfee.scheduler.common.base.exception.Throwables;
import cn.ponfee.scheduler.common.util.Files;
import cn.ponfee.scheduler.common.util.ObjectUtils;
import cn.ponfee.scheduler.core.base.Server;
import cn.ponfee.scheduler.registry.ServerRegistry;
import cn.ponfee.scheduler.registry.zookeeper.configuration.ZookeeperProperties;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Registry server based zookeeper.
 *
 * @author Ponfee
 */
public abstract class ZookeeperServerRegistry<R extends Server, D extends Server> extends ServerRegistry<R, D> {

    private final String registryRootPath;
    private final String discoveryRootPath;

    private final CuratorFrameworkClient client;

    protected ZookeeperServerRegistry(ZookeeperProperties props) {
        this.registryRootPath = Files.UNIX_FOLDER_SEPARATOR + registryRole.key();
        this.discoveryRootPath = Files.UNIX_FOLDER_SEPARATOR + discoveryRole.key();

        try {
            this.client = new CuratorFrameworkClient(props, client0 -> {
                if (closed) {
                    return;
                }
                for (R server : registered) {
                    try {
                        client0.createEphemeral(buildRegistryPath(server));
                    } catch (Exception e) {
                        logger.error("Re-registry server to zookeeper occur error: " + server, e);
                    }
                }
            });
            client.createPersistent(registryRootPath);
            client.createPersistent(discoveryRootPath);
            //client.listenChildChanged(discoveryRootPath);
            client.watchChildChanged(discoveryRootPath, this::refreshDiscoveryServers);
        } catch (Exception e) {
            throw new IllegalStateException("Connect zookeeper failed: " + props, e);
        }
    }

    // ------------------------------------------------------------------Registry

    @Override
    public final void register(R server) {
        if (closed) {
            return;
        }

        try {
            client.createEphemeral(buildRegistryPath(server));
            registered.add(server);
            logger.info("Server registered: {} - {}", registryRole.name(), server);
        } catch (Throwable e) {
            throw new RuntimeException("Register to zookeeper failed: " + server, e);
        }
    }

    @Override
    public final void deregister(R server) {
        String registryPath = buildRegistryPath(server);
        try {
            registered.remove(server);
            client.deletePath(registryPath);
            logger.info("Server deregister: {} - {}", registryRole.name(), server);
        } catch (Throwable e) {
            logger.error("Deregister to zookeeper failed: " + registryPath, e);
        }
    }

    // ------------------------------------------------------------------Subscribe

    /**
     * Refresh discovery servers.
     *
     * @param servers discovered servers
     */
    protected abstract void doRefreshDiscoveryServers(List<D> servers);

    @Override
    public boolean isAlive(D server) {
        return getServers().contains(server);
    }

    @Override
    public void close() {
        closed = true;
        if (!close.compareAndSet(false, true)) {
            logger.warn("Repeat call close method\n{}", ObjectUtils.getStackTrace());
            return;
        }

        registered.forEach(this::deregister);
        Throwables.cached(client::close);
        registered.clear();
    }

    // ------------------------------------------------------------------private registry methods

    private String buildRegistryPath(R server) {
        return registryRootPath + Files.UNIX_FOLDER_SEPARATOR + server.serialize();
    }

    private void refreshDiscoveryServers(List<String> list) {
        List<D> servers;
        logger.info("Watched servers: " + list);
        if (CollectionUtils.isEmpty(list)) {
            logger.error("Not discovered available server on zookeeper.");
            servers = Collections.emptyList();
        } else {
            servers = list.stream()
                .filter(Objects::nonNull)
                .map(s -> (D) discoveryRole.deserialize(s))
                .collect(Collectors.toList());
        }
        doRefreshDiscoveryServers(servers);
    }

}
