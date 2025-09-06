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

package cn.ponfee.disjob.registry.rpc;

import cn.ponfee.disjob.common.util.ProxyUtils;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.registry.Discovery;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Discovery ungrouped server rest proxy
 *
 * @author Ponfee
 */
public final class DiscoveryUngroupedServerRestProxy {

    /**
     * Creates ungrouped rpc service client proxy.
     *
     * @param interfaceCls         the interface class
     * @param localServiceProvider the localServiceProvider
     * @param discoverServer       the discoverServer
     * @param restTemplate         the restTemplate
     * @param retry                the retry config
     * @param <T>                  interface type
     * @param <D>                  discovery server type
     * @return rpc service client proxy
     */
    public static <T, D extends Server> T create(Class<T> interfaceCls,
                                                 T localServiceProvider,
                                                 Discovery<D> discoverServer,
                                                 RestTemplate restTemplate,
                                                 RetryProperties retry) {
        InvocationHandler invocationHandler;
        if (localServiceProvider != null) {
            invocationHandler = new UngroupedServerLocalInvocationHandler<>(localServiceProvider);
        } else {
            DiscoveryServerRestTemplate<D> template = new DiscoveryServerRestTemplate<>(discoverServer, restTemplate, retry);
            RequestMapping requestMapping = AnnotationUtils.findAnnotation(interfaceCls, RequestMapping.class);
            String prefixPath = DiscoveryGroupedServerRestProxy.getMappingPath(requestMapping);
            invocationHandler = new UngroupedServerRemoteInvocationHandler<>(template, prefixPath);
        }
        return ProxyUtils.create(invocationHandler, interfaceCls);
    }

    // ------------------------------------------------------------------------private class

    private static class UngroupedServerLocalInvocationHandler<T> implements InvocationHandler {
        private final T localServiceProvider;

        private UngroupedServerLocalInvocationHandler(T localServiceProvider) {
            this.localServiceProvider = Objects.requireNonNull(localServiceProvider);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return method.invoke(localServiceProvider, args);
        }
    }

    private static class UngroupedServerRemoteInvocationHandler<D extends Server> implements InvocationHandler {
        private final DiscoveryServerRestTemplate<D> template;
        private final String prefixPath;

        private UngroupedServerRemoteInvocationHandler(DiscoveryServerRestTemplate<D> template, String prefixPath) {
            this.template = template;
            this.prefixPath = prefixPath;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            DiscoveryGroupedServerRestProxy.Request req = DiscoveryGroupedServerRestProxy.buildRequest(method, prefixPath);
            return template.execute(method, null, req.httpMethod, req.servletPath, args);
        }
    }

}
