/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry.rpc;

import cn.ponfee.disjob.common.base.Symbol.Str;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingConsumer;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingFunction;
import cn.ponfee.disjob.common.util.ProxyUtils;
import cn.ponfee.disjob.common.util.Strings;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.registry.Discovery;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nullable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

/**
 * Discovery server rest proxy
 *
 * <p>Alias for value: {@link AnnotationUtils#findAnnotation(Class, Class)}
 * <pre>{@code
 *   public @interface RequestMapping {
 *       @AliasFor("path")
 *       String[] value() default {};
 *
 *       @AliasFor("value")
 *       String[] path() default {};
 *   }
 * }</pre>
 *
 * <p>Alias for annotation: {@link AnnotatedElementUtils#findMergedAnnotation(AnnotatedElement, Class)}
 * <pre>{@code
 *   public @interface PostMapping {
 *     @AliasFor(annotation = RequestMapping.class)
 *     String name() default "";
 *   }
 * }</pre>
 *
 * @author Ponfee
 */
public final class DiscoveryServerRestProxy {

    private static final ConcurrentMap<Method, Request> METHOD_REQUEST_CACHE = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> GROUP_THREAD_LOCAL = new NamedThreadLocal<>("discovery_rest_proxy");

    /**
     * Creates ungrouped rpc service client proxy.
     *
     * @param interfaceType   the interface class
     * @param discoveryServer the discoveryServer
     * @param restTemplate    the restTemplate
     * @param retry           the retry config
     * @param <T>             interface type
     * @param <D>             discovery server type
     * @return rpc service client proxy
     */
    public static <T, D extends Server> T create(Class<T> interfaceType,
                                                 Discovery<D> discoveryServer,
                                                 RestTemplate restTemplate,
                                                 RetryProperties retry) {
        DiscoveryServerRestTemplate<D> template = new DiscoveryServerRestTemplate<>(discoveryServer, restTemplate, retry);
        String prefixPath = getMappingPath(AnnotationUtils.findAnnotation(interfaceType, RequestMapping.class));
        InvocationHandler ungroupedInvocationHandler = new UngroupedInvocationHandler(template, prefixPath);
        return ProxyUtils.create(ungroupedInvocationHandler, interfaceType);
    }

    /**
     * Creates grouped rpc service client proxy.
     *
     * @param interfaceType        the interface class
     * @param localServiceProvider the localServiceProvider
     * @param serverGroupMatcher   the serverGroupMatcher
     * @param discoveryServer      the discoveryServer
     * @param restTemplate         the restTemplate
     * @param retry                the retry config
     * @param <T>                  interface type
     * @param <D>                  discovery server type
     * @return rpc service client proxy
     */
    public static <T, D extends Server> GroupedServerInvoker<T> create(Class<T> interfaceType,
                                                                       @Nullable T localServiceProvider,
                                                                       Predicate<String> serverGroupMatcher,
                                                                       Discovery<D> discoveryServer,
                                                                       RestTemplate restTemplate,
                                                                       RetryProperties retry) {
        DiscoveryServerRestTemplate<D> template = new DiscoveryServerRestTemplate<>(discoveryServer, restTemplate, retry);
        String prefixPath = getMappingPath(AnnotationUtils.findAnnotation(interfaceType, RequestMapping.class));
        InvocationHandler groupedInvocationHandler = new GroupedInvocationHandler(template, prefixPath);
        T remoteServiceClient = ProxyUtils.create(groupedInvocationHandler, interfaceType);
        return new GroupedServerInvoker<>(localServiceProvider, remoteServiceClient, serverGroupMatcher);
    }

    public static final class GroupedServerInvoker<T> {
        private final T localServiceProvider;
        private final T remoteServiceClient;
        private final Predicate<String> serverGroupMatcher;

        private GroupedServerInvoker(T localServiceProvider, T remoteServiceClient, Predicate<String> serverGroupMatcher) {
            this.localServiceProvider = localServiceProvider;
            this.remoteServiceClient = remoteServiceClient;
            this.serverGroupMatcher = serverGroupMatcher;
        }

        public <R, E extends Throwable> R invoke(String group, ThrowingFunction<T, R, E> function) throws E {
            if (localServiceProvider != null && serverGroupMatcher.test(group)) {
                return function.apply(localServiceProvider);
            } else {
                GROUP_THREAD_LOCAL.set(Objects.requireNonNull(group));
                try {
                    return function.apply(remoteServiceClient);
                } finally {
                    GROUP_THREAD_LOCAL.remove();
                }
            }
        }

        public <E extends Throwable> void invokeWithoutResult(String group, ThrowingConsumer<T, E> consumer) throws E {
            invoke(group, consumer.toFunction(null));
        }
    }

    private abstract static class DiscoveryInvocationHandler implements InvocationHandler {
        private final DiscoveryServerRestTemplate<?> template;
        private final String prefixPath;

        private DiscoveryInvocationHandler(DiscoveryServerRestTemplate<?> template, String prefixPath) {
            this.template = template;
            this.prefixPath = prefixPath;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Request req = buildRequest(prefixPath, method);
            String group = getGroup();
            return template.execute(group, req.path, req.httpMethod, method.getGenericReturnType(), args);
        }

        /**
         * Returns server group
         *
         * @return group
         */
        protected abstract String getGroup();
    }

    private static final class UngroupedInvocationHandler extends DiscoveryInvocationHandler {
        private UngroupedInvocationHandler(DiscoveryServerRestTemplate<?> template, String prefixPath) {
            super(template, prefixPath);
        }

        @Override
        protected String getGroup() {
            return null;
        }
    }

    private static final class GroupedInvocationHandler extends DiscoveryInvocationHandler {
        private GroupedInvocationHandler(DiscoveryServerRestTemplate<?> template, String prefixPath) {
            super(template, prefixPath);
        }

        @Override
        protected String getGroup() {
            return GROUP_THREAD_LOCAL.get();
        }
    }

    static Request buildRequest(String prefixPath, Method method) {
        return METHOD_REQUEST_CACHE.computeIfAbsent(method, key -> {
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(key, RequestMapping.class);
            if (mapping == null || ArrayUtils.isEmpty(mapping.method())) {
                throw new IllegalStateException("Non http mapping method: " + prefixPath + ", " + method.toGenericString());
            }

            String suffixPath = getMappingPath(mapping);
            String urlPath = Strings.concatUrlPath(prefixPath, suffixPath);
            return Arrays.stream(mapping.method())
                .filter(Objects::nonNull)
                .findAny()
                .map(Enum::name)
                .map(HttpMethod::valueOf)
                .map(httpMethod -> new Request(urlPath, httpMethod))
                .orElseThrow(() -> new IllegalStateException("Invalid http mapping method: " + urlPath + ", " + method.toGenericString()));
        });
    }

    static class Request {
        final String path;
        final HttpMethod httpMethod;

        private Request(String path, HttpMethod httpMethod) {
            this.path = path;
            this.httpMethod = httpMethod;
        }
    }

    /**
     * Gets annotated spring web mvc {@link RequestMapping} method path
     *
     * @param mapping the request mapping annotation
     * @return path
     */
    static String getMappingPath(RequestMapping mapping) {
        if (mapping == null) {
            return Str.SLASH;
        }
        String firstPath = Collects.get(mapping.path(), 0);
        return Strings.trimUrlPath(firstPath);
    }

}
