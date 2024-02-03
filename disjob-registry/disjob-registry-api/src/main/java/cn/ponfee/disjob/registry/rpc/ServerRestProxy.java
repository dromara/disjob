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
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.Server;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Server rest proxy
 *
 * @author Ponfee
 */
public class ServerRestProxy {

    private static final ThreadLocal<Server> SERVER_THREAD_LOCAL = new NamedThreadLocal<>("server_rest_proxy");

    public static <T> DesignatedServerInvoker<T> create(Class<T> interfaceType,
                                                        T localServiceProvider,
                                                        Server currentServer,
                                                        RestTemplate restTemplate,
                                                        RetryProperties retry) {
        ServerRestTemplate serverRestTemplate = new ServerRestTemplate(restTemplate, retry);
        String prefixPath = DiscoveryRestProxy.parsePath(AnnotationUtils.findAnnotation(interfaceType, RequestMapping.class));
        InvocationHandler invocationHandler = new ServerInvocationHandler(serverRestTemplate, prefixPath);
        T remoteServiceClient = ProxyUtils.create(invocationHandler, interfaceType);
        return new DesignatedServerInvoker<>(remoteServiceClient, localServiceProvider, currentServer);
    }

    public static final class DesignatedServerInvoker<T> {
        private final T remoteServiceClient;
        private final T localServiceProvider;
        private final Server currentServer;

        private DesignatedServerInvoker(T remoteServiceClient, T localServiceProvider, Server currentServer) {
            this.remoteServiceClient = remoteServiceClient;
            this.localServiceProvider = localServiceProvider;
            this.currentServer = currentServer;
        }

        public <R> R invoke(Server destinationServer, Function<T, R> function) {
            if (localServiceProvider != null && destinationServer.equals(currentServer)) {
                return function.apply(localServiceProvider);
            } else {
                SERVER_THREAD_LOCAL.set(Objects.requireNonNull(destinationServer));
                try {
                    return function.apply(remoteServiceClient);
                } finally {
                    SERVER_THREAD_LOCAL.remove();
                }
            }
        }

        public void invokeWithoutResult(Server destinationServer, Consumer<T> consumer) {
            invoke(destinationServer, Functions.convert(consumer, null));
        }
    }

    private static class ServerInvocationHandler implements InvocationHandler {
        private final ServerRestTemplate serverRestTemplate;
        private final String prefixPath;

        private ServerInvocationHandler(ServerRestTemplate serverRestTemplate, String prefixPath) {
            this.serverRestTemplate = serverRestTemplate;
            this.prefixPath = prefixPath;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            DiscoveryRestProxy.Request req = DiscoveryRestProxy.buildRequest(prefixPath, method);
            Server destinationServer = SERVER_THREAD_LOCAL.get();
            return serverRestTemplate.invoke(destinationServer, req.path, req.httpMethod, method.getGenericReturnType(), args);
        }
    }

}
