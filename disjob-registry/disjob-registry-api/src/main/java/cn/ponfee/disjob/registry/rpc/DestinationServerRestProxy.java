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

import cn.ponfee.disjob.common.exception.Throwables.ThrowingConsumer;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingFunction;
import cn.ponfee.disjob.common.util.ProxyUtils;
import cn.ponfee.disjob.common.util.Strings;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.Server;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Function;

/**
 * Destination(Designated) server rest proxy
 *
 * @author Ponfee
 */
public final class DestinationServerRestProxy {

    private static final ThreadLocal<Server> SERVER_THREAD_LOCAL = new NamedThreadLocal<>("destination-server");

    public static <T, S extends Server> DestinationServerInvoker<T, S> create(Class<T> interfaceCls,
                                                                              @Nullable T localServiceProvider,
                                                                              @Nullable S localServer,
                                                                              Function<S, String> serverContextPath,
                                                                              RestTemplate restTemplate,
                                                                              RetryProperties retry) {
        DestinationServerRestTemplate template = new DestinationServerRestTemplate(restTemplate, retry);
        String prefixPath = DiscoveryServerRestProxy.getMappingPath(AnnotationUtils.findAnnotation(interfaceCls, RequestMapping.class));
        InvocationHandler invocationHandler = new ServerInvocationHandler<>(template, serverContextPath, prefixPath);
        T remoteServiceClient = ProxyUtils.create(invocationHandler, interfaceCls);
        return new DestinationServerInvoker<>(remoteServiceClient, localServiceProvider, localServer);
    }

    public static final class DestinationServerInvoker<T, S extends Server> {
        private final T remoteServiceClient;
        private final T localServiceProvider;
        private final S localServer;

        private DestinationServerInvoker(T remoteServiceClient, T localServiceProvider, S localServer) {
            this.remoteServiceClient = remoteServiceClient;
            this.localServiceProvider = localServiceProvider;
            this.localServer = localServer;
        }

        public <R, E extends Throwable> R call(S destinationServer, ThrowingFunction<T, R, E> function) throws E {
            Objects.requireNonNull(destinationServer);
            if (localServiceProvider != null && destinationServer.equals(localServer)) {
                return function.apply(localServiceProvider);
            } else {
                SERVER_THREAD_LOCAL.set(destinationServer);
                try {
                    return function.apply(remoteServiceClient);
                } finally {
                    SERVER_THREAD_LOCAL.remove();
                }
            }
        }

        public <E extends Throwable> void invoke(S destinationServer, ThrowingConsumer<T, E> consumer) throws E {
            call(destinationServer, consumer.toFunction(null));
        }
    }

    private static class ServerInvocationHandler<S extends Server> implements InvocationHandler {
        private final DestinationServerRestTemplate template;
        private final Function<S, String> serverContextPath;
        private final String prefixPath;

        private ServerInvocationHandler(DestinationServerRestTemplate template,
                                        Function<S, String> serverContextPath,
                                        String prefixPath) {
            this.template = template;
            this.serverContextPath = serverContextPath;
            this.prefixPath = prefixPath;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            DiscoveryServerRestProxy.Request req = DiscoveryServerRestProxy.buildRequest(prefixPath, method);
            Server destinationServer = SERVER_THREAD_LOCAL.get();
            String contextPath = serverContextPath.apply((S) destinationServer);
            String urlPath = Strings.concatPath(contextPath, req.path);
            return template.invoke(destinationServer, urlPath, req.httpMethod, method.getGenericReturnType(), args);
        }
    }

}
