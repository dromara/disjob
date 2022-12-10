package cn.ponfee.scheduler.registry.nacos;

import cn.ponfee.scheduler.common.base.exception.Throwables;
import cn.ponfee.scheduler.common.util.ObjectUtils;
import cn.ponfee.scheduler.core.base.Server;
import cn.ponfee.scheduler.registry.ServerRegistry;
import cn.ponfee.scheduler.registry.nacos.configuration.NacosProperties;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * Registry server based <a href="https://nacos.io/zh-cn/docs/what-is-nacos.html">nacos</a>.
 *
 * @author Ponfee
 */
public abstract class NacosServerRegistry<R extends Server, D extends Server> extends ServerRegistry<R, D> {

    private final String groupName;

    /**
     * Nacos naming service
     */
    private final NamingService namingService;

    /**
     * Nacos event listener
     */
    private final EventListener eventListener;

    protected NacosServerRegistry(String namespace, NacosProperties config) {
        super(namespace, ':');
        this.groupName = StringUtils.isBlank(namespace) ? Constants.DEFAULT_GROUP : namespace.trim();

        CountDownLatch latch = new CountDownLatch(1);
        try {
            this.namingService = NacosFactory.createNamingService(config.toProperties());
            this.eventListener = event -> {
                Throwables.caught(latch::await);
                if (event instanceof NamingEvent) {
                    doRefreshDiscoveryServers(((NamingEvent) event).getInstances());
                }
            };
            namingService.subscribe(discoveryRootPath, groupName, eventListener);
            doRefreshDiscoveryServers(namingService.selectInstances(discoveryRootPath, groupName, true));
        } catch (NacosException e) {
            throw new IllegalStateException(e);
        } finally {
            latch.countDown();
        }
    }

    // ------------------------------------------------------------------Registry

    @Override
    public final boolean isConnected() {
        return "UP".equals(namingService.getServerStatus());
    }

    @Override
    public final void register(R server) {
        if (closed) {
            return;
        }

        Instance instance = createInstance(server);
        try {
            namingService.registerInstance(registryRootPath, groupName, instance);
            registered.add(server);
            log.info("Nacos server registered: {} - {}", registryRole.name(), server);
        } catch (Throwable e) {
            throw new RuntimeException("Nacos server registered failed: " + server, e);
        }
    }

    @Override
    public final void deregister(R server) {
        Instance instance = createInstance(server);
        try {
            registered.remove(server);
            namingService.deregisterInstance(registryRootPath, groupName, instance);
            log.info("Nacos server deregister: {} - {}", registryRole.name(), server);
        } catch (Exception e) {
            log.error("Nacos server deregister error.", e);
        }
    }

    // ------------------------------------------------------------------Close

    @Override
    public void close() {
        closed = true;
        if (!close.compareAndSet(false, true)) {
            log.warn("Repeat call close method\n{}", ObjectUtils.getStackTrace());
            return;
        }

        Throwables.caught(() -> namingService.unsubscribe(discoveryRootPath, groupName, eventListener));
        registered.forEach(this::deregister);
        registered.clear();
        Throwables.caught(namingService::shutDown);
        super.close();
    }

    // ------------------------------------------------------------------private method

    private Instance createInstance(R server) {
        Instance instance = new Instance();
        instance.setInstanceId(server.serialize());
        instance.setIp(server.getHost());
        instance.setPort(server.getPort());
        return instance;
    }

    private synchronized void doRefreshDiscoveryServers(List<Instance> instances) {
        List<D> servers;
        if (CollectionUtils.isEmpty(instances)) {
            log.error("Not discovered available {} from nacos.", discoveryRole.name());
            servers = Collections.emptyList();
        } else {
            servers = instances.stream()
                .map(Instance::getInstanceId)
                .filter(Objects::nonNull)
                .map(s -> (D) discoveryRole.deserialize(s))
                .collect(Collectors.toList());
        }
        refreshDiscoveredServers(servers);
    }

}
