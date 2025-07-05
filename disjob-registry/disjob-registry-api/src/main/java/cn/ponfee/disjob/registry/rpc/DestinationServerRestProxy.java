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
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Destination(Designated) server rest proxy
 *
 * @author Ponfee
 */
public final class DestinationServerRestProxy<T, S extends Server> {

    private final Constructor<T> constructor;
    private final T localServiceProvider;
    private final S localServer;
    private final DestinationServerRestTemplate template;
    private final String prefixPath;

    private DestinationServerRestProxy(Constructor<T> constructor,
                                       T localServiceProvider,
                                       S localServer,
                                       DestinationServerRestTemplate template,
                                       String prefixPath) {
        this.constructor = constructor;
        this.localServiceProvider = localServiceProvider;
        this.localServer = localServer;
        this.template = template;
        this.prefixPath = prefixPath;
    }

    public T destination(S destinationServer) {
        try {
            return constructor.newInstance(new DestinationServerInvocationHandler(destinationServer));
        } catch (Exception e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public static <T, S extends Server> DestinationServerRestProxy<T, S> of(Class<T> interfaceCls,
                                                                            @Nullable T localServiceProvider,
                                                                            @Nullable S localServer,
                                                                            RestTemplate restTemplate,
                                                                            RetryProperties retry) {
        Constructor<T> constructor = ProxyUtils.getProxyConstructor(interfaceCls);
        DestinationServerRestTemplate template = new DestinationServerRestTemplate(restTemplate, retry);
        RequestMapping requestMapping = AnnotationUtils.findAnnotation(interfaceCls, RequestMapping.class);
        String prefixPath = DiscoveryGroupedServerRestProxy.getMappingPath(requestMapping);
        return new DestinationServerRestProxy<>(constructor, localServiceProvider, localServer, template, prefixPath);
    }

    // ------------------------------------------------------------------------private class

    private class DestinationServerInvocationHandler implements InvocationHandler {
        private final S destinationServer;

        private DestinationServerInvocationHandler(S destinationServer) {
            this.destinationServer = Objects.requireNonNull(destinationServer);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (localServiceProvider != null && destinationServer.equals(localServer)) {
                return method.invoke(localServiceProvider, args);
            } else {
                DiscoveryGroupedServerRestProxy.Request req = DiscoveryGroupedServerRestProxy.buildRequest(method, prefixPath);
                return template.invoke(method, destinationServer, req.httpMethod, req.servletPath, args);
            }
        }
    }

}
