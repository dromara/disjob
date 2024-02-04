/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry.rpc;

import cn.ponfee.disjob.common.util.Functions;
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
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Destination server rest proxy
 *
 * @author Ponfee
 */
public class DestinationServerRestProxy {

    private static final ThreadLocal<Server> SERVER_THREAD_LOCAL = new NamedThreadLocal<>("server_rest_proxy");

    public static <T, S extends Server> DestinationServerInvoker<T, S> create(Class<T> interfaceType,
                                                                              @Nullable T localServiceProvider,
                                                                              @Nullable S currentServer,
                                                                              Function<S, String> serverContextPath,
                                                                              RestTemplate restTemplate,
                                                                              RetryProperties retry) {
        DestinationServerRestTemplate template = new DestinationServerRestTemplate(restTemplate, retry);
        String prefixPath = DiscoveryServerRestProxy.getMappingPath(AnnotationUtils.findAnnotation(interfaceType, RequestMapping.class));
        InvocationHandler invocationHandler = new ServerInvocationHandler<>(template, serverContextPath, prefixPath);
        T remoteServiceClient = ProxyUtils.create(invocationHandler, interfaceType);
        return new DestinationServerInvoker<>(remoteServiceClient, localServiceProvider, currentServer);
    }

    public static final class DestinationServerInvoker<T, S extends Server> {
        private final T remoteServiceClient;
        private final T localServiceProvider;
        private final S currentServer;

        private DestinationServerInvoker(T remoteServiceClient, T localServiceProvider, S currentServer) {
            this.remoteServiceClient = remoteServiceClient;
            this.localServiceProvider = localServiceProvider;
            this.currentServer = currentServer;
        }

        public <R> R invoke(S destinationServer, Function<T, R> function) {
            Objects.requireNonNull(destinationServer);
            if (localServiceProvider != null && destinationServer.equals(currentServer)) {
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

        public void invokeWithoutResult(S destinationServer, Consumer<T> consumer) {
            invoke(destinationServer, Functions.convert(consumer, null));
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
            String urlPath = Strings.concatUrlPath(contextPath, req.path);
            return template.invoke(destinationServer, urlPath, req.httpMethod, method.getGenericReturnType(), args);
        }
    }

}
