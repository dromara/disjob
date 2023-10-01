/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry.nacos;

import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.util.ObjectUtils;
import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.registry.RegistryException;
import cn.ponfee.disjob.registry.ServerRegistry;
import cn.ponfee.disjob.registry.nacos.configuration.NacosRegistryProperties;
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

    protected NacosServerRegistry(NacosRegistryProperties config) {
        super(config.getNamespace(), ':');
        this.groupName = StringUtils.isBlank(config.getNamespace()) ? Constants.DEFAULT_GROUP : config.getNamespace().trim();

        CountDownLatch latch = new CountDownLatch(1);
        try {
            this.namingService = NacosFactory.createNamingService(config.toProperties());
            this.eventListener = event -> {
                ThrowingRunnable.execute(latch::await);
                if (event instanceof NamingEvent) {
                    doRefreshDiscoveryServers(((NamingEvent) event).getInstances());
                }
            };
            namingService.subscribe(discoveryRootPath, groupName, eventListener);
            doRefreshDiscoveryServers(namingService.selectInstances(discoveryRootPath, groupName, true));
        } catch (NacosException e) {
            throw new RegistryException("Nacos registry init error: " + config, e);
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
        if (closed.get()) {
            return;
        }

        Instance instance = createInstance(server);
        try {
            namingService.registerInstance(registryRootPath, groupName, instance);
            registered.add(server);
            log.info("Nacos server registered: {} | {}", registryRole.name(), server);
        } catch (Throwable e) {
            throw new RegistryException("Nacos server register failed: " + server, e);
        }
    }

    @Override
    public final void deregister(R server) {
        Instance instance = createInstance(server);
        try {
            registered.remove(server);
            namingService.deregisterInstance(registryRootPath, groupName, instance);
            log.info("Nacos server deregister: {} | {}", registryRole.name(), server);
        } catch (Throwable t) {
            log.error("Nacos server deregister error.", t);
        }
    }

    // ------------------------------------------------------------------Close

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            log.warn("Repeat call close method\n{}", ObjectUtils.getStackTrace());
            return;
        }

        registered.forEach(this::deregister);
        registered.clear();
        ThrowingRunnable.execute(() -> namingService.unsubscribe(discoveryRootPath, groupName, eventListener));
        ThrowingRunnable.execute(namingService::shutDown);
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
            log.warn("Not discovered available {} from nacos.", discoveryRole.name());
            servers = Collections.emptyList();
        } else {
            servers = instances.stream()
                .map(Instance::getInstanceId)
                .filter(Objects::nonNull)
                .<D>map(discoveryRole::deserialize)
                .collect(Collectors.toList());
        }
        refreshDiscoveredServers(servers);
    }

}
