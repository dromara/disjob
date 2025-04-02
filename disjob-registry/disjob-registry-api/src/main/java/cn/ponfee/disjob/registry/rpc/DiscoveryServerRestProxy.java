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

import cn.ponfee.disjob.common.base.RetryInvocationHandler;
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
 * <p>{@link AnnotationUtils#findAnnotation(Class, Class)}
 * <p>在提供的类上查找指定注解的单个注解，如果注解不直接出现在提供的类上，则遍历其注解的元注解(如`@RpcController`上的元注解)、接口和超类。
 * <pre>{@code
 *   @RpcController("super")
 *   class SupClass { }
 *
 *   class SubClass extends SupClass { }
 *
 *   // 不支持`@AliasFor`语义
 *   assertEquals("", AnnotationUtils.findAnnotation(SubClass.class, Component.class).value());
 *   assertEquals("super", AnnotatedElementUtils.findMergedAnnotation(SubClass.class, Component.class).value());
 * }</pre>
 *
 * <p>{@link AnnotatedElementUtils#findMergedAnnotation(AnnotatedElement, Class)}
 * <p>在提供的元素上方的注解层次结构中查找指定注解类型的第一个注解，将注解的属性与注解层次结构的较低级别中的注解的匹配属性合并，并将结果合成为指定注解类型的注解。
 * <p>@AliasFor语义在单个注解和注解层次结构中都完全受支持。
 * <pre>{@code
 *   class HelloController {
 *     @GetMapping("/hello")
 *     public String hello(String name) {
 *       return "Hello " + name + "!";
 *     }
 *   }
 *
 *   Method method = HelloController.class.getMethod("hello", String.class);
 *   RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
 *   assertEquals(1, mapping.value().length);
 *   assertEquals("/hello", mapping.value()[0]);
 * }</pre>
 *
 * @author Ponfee
 */
public final class DiscoveryServerRestProxy {

    private static final ConcurrentMap<Method, Request> METHOD_REQUEST_CACHE = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> GROUP_THREAD_LOCAL = new NamedThreadLocal<>("discovery-group");

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
                                                 @Nullable T localServiceProvider,
                                                 Discovery<D> discoverServer,
                                                 RestTemplate restTemplate,
                                                 RetryProperties retry) {
        InvocationHandler invocationHandler;
        if (localServiceProvider != null) {
            // 本地调用：使用动态代理来增加重试能力
            invocationHandler = new RetryInvocationHandler(localServiceProvider, retry.getMaxCount(), retry.getBackoffPeriod());
        } else {
            // 远程调用：通过Discovery<D>来获取目标服务器
            DiscoveryServerRestTemplate<D> template = new DiscoveryServerRestTemplate<>(discoverServer, restTemplate, retry);
            String prefixPath = getMappingPath(AnnotationUtils.findAnnotation(interfaceCls, RequestMapping.class));
            invocationHandler = new UngroupedInvocationHandler(template, prefixPath);
        }
        return ProxyUtils.create(invocationHandler, interfaceCls);
    }

    /**
     * Creates grouped rpc service client proxy.
     *
     * @param interfaceCls         the interface class
     * @param localServiceProvider the localServiceProvider
     * @param serverGroupMatcher   the serverGroupMatcher
     * @param discoverServer       the discoverServer
     * @param restTemplate         the restTemplate
     * @param retry                the retry config
     * @param <T>                  interface type
     * @param <D>                  discovery server type
     * @return rpc service client proxy
     */
    public static <T, D extends Server> GroupedServerClient<T> create(Class<T> interfaceCls,
                                                                      @Nullable T localServiceProvider,
                                                                      Predicate<String> serverGroupMatcher,
                                                                      Discovery<D> discoverServer,
                                                                      RestTemplate restTemplate,
                                                                      RetryProperties retry) {
        DiscoveryServerRestTemplate<D> template = new DiscoveryServerRestTemplate<>(discoverServer, restTemplate, retry);
        String prefixPath = getMappingPath(AnnotationUtils.findAnnotation(interfaceCls, RequestMapping.class));
        InvocationHandler groupedInvocationHandler = new GroupedInvocationHandler(template, prefixPath);
        T remoteServiceClient = ProxyUtils.create(groupedInvocationHandler, interfaceCls);
        return new GroupedServerClient<>(localServiceProvider, remoteServiceClient, serverGroupMatcher);
    }

    public static final class GroupedServerClient<T> {
        private final T localServiceProvider;
        private final T remoteServiceClient;
        private final Predicate<String> serverGroupMatcher;

        private GroupedServerClient(T localServiceProvider, T remoteServiceClient, Predicate<String> serverGroupMatcher) {
            this.localServiceProvider = localServiceProvider;
            this.remoteServiceClient = remoteServiceClient;
            this.serverGroupMatcher = serverGroupMatcher;
        }

        public <R, E extends Throwable> R call(String group, ThrowingFunction<T, R, E> function) throws E {
            Objects.requireNonNull(group);
            if (localServiceProvider != null && serverGroupMatcher.test(group)) {
                return function.apply(localServiceProvider);
            } else {
                GROUP_THREAD_LOCAL.set(group);
                try {
                    return function.apply(remoteServiceClient);
                } finally {
                    GROUP_THREAD_LOCAL.remove();
                }
            }
        }

        public <E extends Throwable> void invoke(String group, ThrowingConsumer<T, E> consumer) throws E {
            call(group, consumer.toFunction(null));
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
            Request req = buildRequest(method, prefixPath);
            String group = getGroup();
            return template.execute(method, group, req.httpMethod, req.path, args);
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

    static Request buildRequest(final Method method, final String prefixPath) {
        // 如果是继承方式，两个子接口继承的`subscribeServerEvent`方法的prefixPath不一样，Map Key需要改为`Pair<Class<?>, Method>`
        // SubscribeEventService {
        //   void subscribeServerEvent(RegistryEventType eventType, Server server);
        // }
        //
        // @RequestMapping("/worker/rpc")
        // WorkerRpcService extends SubscribeEventService { }
        //
        // @RequestMapping("/supervisor/rpc")
        // SupervisorRpcService extends SubscribeEventService { }
        //
        return METHOD_REQUEST_CACHE.computeIfAbsent(method, key -> {
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(key, RequestMapping.class);
            if (mapping == null || ArrayUtils.isEmpty(mapping.method())) {
                throw new IllegalStateException("Non http mapping method: " + key.toGenericString());
            }

            String suffixPath = getMappingPath(mapping);
            String urlPath = Strings.concatPath(prefixPath, suffixPath);
            return Arrays.stream(mapping.method())
                .filter(Objects::nonNull)
                .findAny()
                .map(Enum::name)
                .map(HttpMethod::valueOf)
                .map(httpMethod -> new Request(urlPath, httpMethod))
                .orElseThrow(() -> new IllegalStateException("Invalid http mapping method: " + key.toGenericString()));
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
        return Strings.trimPath(firstPath);
    }

}
