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
import cn.ponfee.disjob.common.util.Files;
import cn.ponfee.disjob.common.util.ProxyUtils;
import cn.ponfee.disjob.core.base.HttpProperties;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.registry.Discovery;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Discovery rest proxy
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
public class DiscoveryRestProxy {

    private static final Map<Method, Request> METHOD_REQUEST_CACHE = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> GROUP_THREAD_LOCAL = new NamedThreadLocal<>("discovery_rest_proxy");

    /**
     * Creates ungrouped rpc service client proxy.
     *
     * @param interfaceType   the interface class
     * @param http            the http config
     * @param retry           the retry config
     * @param objectMapper    the objectMapper
     * @param discoveryServer the discoveryServer
     * @param <T>             interface type
     * @param <D>             discovery server type
     * @return rpc service client proxy
     */
    public static <T, D extends Server> T create(Class<T> interfaceType,
                                                 HttpProperties http,
                                                 RetryProperties retry,
                                                 ObjectMapper objectMapper,
                                                 Discovery<D> discoveryServer) {
        DiscoveryRestTemplate<D> restTemplate = new DiscoveryRestTemplate<>(http, retry, objectMapper, discoveryServer);
        String prefixPath = parsePath(AnnotationUtils.findAnnotation(interfaceType, RequestMapping.class));
        InvocationHandler invocationHandler = new UngroupedInvocationHandler(restTemplate, prefixPath);
        return ProxyUtils.create(invocationHandler, interfaceType);
    }

    /**
     * Creates grouped rpc service client proxy.
     *
     * @param interfaceType             the interface class
     * @param localServiceProvider      the localServiceProvider
     * @param currentServerGroupMatcher the currentServerGroupMatcher
     * @param http                      the http config
     * @param retry                     the retry config
     * @param objectMapper              the objectMapper
     * @param discoveryServer           the discoveryServer
     * @param <T>                       interface type
     * @param <D>                       discovery server type
     * @return rpc service client proxy
     */
    public static <T, D extends Server> GroupedServerInvoker<T> create(Class<T> interfaceType,
                                                                       T localServiceProvider,
                                                                       Predicate<String> currentServerGroupMatcher,
                                                                       HttpProperties http,
                                                                       RetryProperties retry,
                                                                       ObjectMapper objectMapper,
                                                                       Discovery<D> discoveryServer) {
        DiscoveryRestTemplate<D> restTemplate = new DiscoveryRestTemplate<>(http, retry, objectMapper, discoveryServer);
        String prefixPath = parsePath(AnnotationUtils.findAnnotation(interfaceType, RequestMapping.class));
        InvocationHandler invocationHandler = new GroupedInvocationHandler(restTemplate, prefixPath);
        T remoteServiceClient = ProxyUtils.create(invocationHandler, interfaceType);
        return new GroupedServerInvoker<>(localServiceProvider, remoteServiceClient, currentServerGroupMatcher);
    }

    public static final class GroupedServerInvoker<T> {
        private final T localServiceProvider;
        private final T remoteServiceClient;
        private final Predicate<String> currentServerGroupMatcher;

        public GroupedServerInvoker(T localServiceProvider, T remoteServiceClient, Predicate<String> currentServerGroupMatcher) {
            this.localServiceProvider = localServiceProvider;
            this.remoteServiceClient = remoteServiceClient;
            this.currentServerGroupMatcher = currentServerGroupMatcher;
        }

        public <R> R invoke(String group, ThrowingFunction<T, R, ?> function) {
            if (localServiceProvider != null && currentServerGroupMatcher.test(group)) {
                return ThrowingFunction.doChecked(function, localServiceProvider);
            } else {
                GROUP_THREAD_LOCAL.set(Objects.requireNonNull(group));
                try {
                    return ThrowingFunction.doChecked(function, remoteServiceClient);
                } finally {
                    GROUP_THREAD_LOCAL.remove();
                }
            }
        }

        public void invokeWithoutResult(String group, ThrowingConsumer<T, ?> consumer) {
            invoke(group, consumer.toFunction(null));
        }
    }

    private abstract static class DiscoveryInvocationHandler implements InvocationHandler {
        private final DiscoveryRestTemplate<?> discoveryRestTemplate;
        private final String prefixPath;

        private DiscoveryInvocationHandler(DiscoveryRestTemplate<?> discoveryRestTemplate, String prefixPath) {
            this.discoveryRestTemplate = discoveryRestTemplate;
            this.prefixPath = prefixPath;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Request request = buildRequest(prefixPath, method);
            Objects.requireNonNull(request, () -> "Invalid discovery http request method: " + method.toGenericString());
            return discoveryRestTemplate.execute(getGroup(), request.path, request.httpMethod, method.getGenericReturnType(), args);
        }

        /**
         * Returns server group
         *
         * @return group
         */
        protected abstract String getGroup();
    }

    private static final class UngroupedInvocationHandler extends DiscoveryInvocationHandler {

        private UngroupedInvocationHandler(DiscoveryRestTemplate<?> discoveryRestTemplate, String prefixPath) {
            super(discoveryRestTemplate, prefixPath);
        }

        @Override
        protected String getGroup() {
            return null;
        }
    }

    private static final class GroupedInvocationHandler extends DiscoveryInvocationHandler {

        private GroupedInvocationHandler(DiscoveryRestTemplate<?> discoveryRestTemplate, String prefixPath) {
            super(discoveryRestTemplate, prefixPath);
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
                return null;
            }
            String suffixPath = parsePath(mapping), fullPath;
            if (prefixPath.isEmpty()) {
                fullPath = suffixPath.isEmpty() ? null : suffixPath;
            } else {
                fullPath = suffixPath.isEmpty() ? prefixPath : prefixPath + Files.UNIX_FOLDER_SEPARATOR + suffixPath;
            }
            if (fullPath == null) {
                return null;
            }
            return Arrays.stream(mapping.method())
                .filter(Objects::nonNull)
                .map(Enum::name)
                .map(HttpMethod::valueOf)
                .map(httpMethod -> new Request(fullPath, httpMethod))
                .findAny()
                .orElse(null);
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
     * Parse annotated spring web {@link RequestMapping} method path
     *
     * @param mapping the request mapping
     * @return path
     */
    static String parsePath(RequestMapping mapping) {
        String path;
        if (mapping == null || StringUtils.isEmpty(path = Collects.get(mapping.path(), 0))) {
            return "";
        }
        if (path.startsWith(Str.SLASH)) {
            path = path.substring(1);
        }
        if (path.endsWith(Str.SLASH)) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

}
