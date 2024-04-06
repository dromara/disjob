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

package cn.ponfee.disjob.registry.nacos;

import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
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

import javax.annotation.PreDestroy;
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
                ThrowingRunnable.doCaught(latch::await);
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
        if (state.isStopped()) {
            return;
        }

        Instance instance = createInstance(server);
        try {
            namingService.registerInstance(registryRootPath, groupName, instance);
            registered.add(server);
            log.info("Nacos server registered: {}, {}", registryRole, server);
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
            log.info("Nacos server deregister: {}, {}", registryRole, server);
        } catch (Throwable t) {
            log.error("Nacos server deregister error.", t);
        }
    }

    @Override
    public List<R> getRegisteredServers() throws Exception {
        List<Instance> list = namingService.getAllInstances(registryRootPath, groupName);
        return deserializeRegistryServers(list, Instance::getInstanceId);
    }

    // ------------------------------------------------------------------Close

    @PreDestroy
    @Override
    public void close() {
        if (!state.stop()) {
            return;
        }

        registered.forEach(this::deregister);
        registered.clear();
        ThrowingRunnable.doCaught(() -> namingService.unsubscribe(discoveryRootPath, groupName, eventListener));
        ThrowingRunnable.doCaught(namingService::shutDown);
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
            log.warn("Not discovered available {} from nacos.", discoveryRole);
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
