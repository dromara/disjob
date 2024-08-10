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
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * Nacos client
 *
 * @author Ponfee
 */
public class NacosClient {

    private final NamingService namingService;
    private final String groupName;
    private final Map<String, Watcher> watchers = new ConcurrentHashMap<>();

    public NacosClient(Properties config, String groupName) throws NacosException {
        this.namingService = NacosFactory.createNamingService(config);
        this.groupName = groupName;
    }

    public synchronized void watch(String serviceName, Consumer<List<Instance>> listener) throws NacosException {
        if (watchers.containsKey(serviceName)) {
            throw new IllegalStateException("Service name already watched: " + serviceName);
        }

        CountDownLatch latch = new CountDownLatch(1);
        try {
            Watcher watcher = new Watcher(listener, latch);
            namingService.subscribe(serviceName, groupName, watcher);
            listener.accept(namingService.selectInstances(serviceName, groupName, true));
            watchers.put(serviceName, watcher);
        } finally {
            latch.countDown();
        }
    }

    public synchronized void unwatch(String serviceName) {
        Watcher watcher = watchers.remove(serviceName);
        if (watcher != null) {
            ThrowingRunnable.doCaught(() -> namingService.unsubscribe(serviceName, groupName, watcher));
        }
    }

    public String getServerStatus() {
        return namingService.getServerStatus();
    }

    public void registerInstance(String serviceName, Instance instance) throws NacosException {
        namingService.registerInstance(serviceName, groupName, instance);
    }

    public void deregisterInstance(String serviceName, Instance instance) throws NacosException {
        namingService.deregisterInstance(serviceName, groupName, instance);
    }

    public List<Instance> getAllInstances(String serviceName) throws NacosException {
        return namingService.getAllInstances(serviceName, groupName);
    }

    public synchronized void close() {
        new ArrayList<>(watchers.keySet()).forEach(this::unwatch);
        ThrowingRunnable.doCaught(namingService::shutDown);
    }

    private static class Watcher implements EventListener {
        private final Consumer<List<Instance>> listener;
        private final CountDownLatch latch;

        Watcher(Consumer<List<Instance>> listener, CountDownLatch latch) {
            this.listener = listener;
            this.latch = latch;
        }

        @Override
        public void onEvent(Event event) {
            if (event instanceof NamingEvent) {
                ThrowingRunnable.doCaught(latch::await);
                listener.accept(((NamingEvent) event).getInstances());
            }
        }
    }

}
