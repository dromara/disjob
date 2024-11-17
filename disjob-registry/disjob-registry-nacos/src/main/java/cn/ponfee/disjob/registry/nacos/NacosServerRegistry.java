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

import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.registry.RegistryException;
import cn.ponfee.disjob.registry.ServerRegistry;
import cn.ponfee.disjob.registry.nacos.configuration.NacosRegistryProperties;
import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Registry server based <a href="https://nacos.io/zh-cn/docs/what-is-nacos.html">nacos</a>.
 *
 * @author Ponfee
 */
public abstract class NacosServerRegistry<R extends Server, D extends Server> extends ServerRegistry<R, D> {

    /**
     * Nacos client
     */
    private final NacosClient client;

    protected NacosServerRegistry(NacosRegistryProperties config) {
        super(config, ':');
        String groupName = StringUtils.isBlank(config.getNamespace()) ? Constants.DEFAULT_GROUP : config.getNamespace().trim();
        NacosClient client0 = null;
        try {
            this.client = client0 = new NacosClient(config.toProperties(), groupName);
            client.watch(discoveryRootPath, instances -> refreshDiscoveryServers(extract(instances)));
        } catch (Throwable t) {
            if (client0 != null) {
                client0.close();
            }
            throw new RegistryException("Nacos registry init error: " + config, t);
        }
    }

    // ------------------------------------------------------------------Registry

    @Override
    public final boolean isConnected() {
        return "UP".equals(client.getServerStatus());
    }

    @Override
    public final void register(R server) {
        if (state.isStopped()) {
            return;
        }

        Instance instance = createInstance(server);
        try {
            client.registerInstance(registryRootPath, instance);
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
            client.deregisterInstance(registryRootPath, instance);
            log.info("Nacos server deregister: {}, {}", registryRole, server);
        } catch (Throwable t) {
            log.error("Nacos server deregister error.", t);
        }
    }

    @Override
    public List<R> getRegisteredServers() {
        try {
            List<Instance> instances = client.getAllInstances(registryRootPath);
            return deserializeServers(extract(instances), registryRole);
        } catch (NacosException e) {
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
        client.close();
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

    private List<String> extract(List<Instance> list) {
        if (list == null) {
            return null;
        }
        return list.stream().filter(Objects::nonNull).map(Instance::getInstanceId).collect(Collectors.toList());
    }

}
