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

import cn.ponfee.disjob.common.base.Symbol.Str;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.util.ProxyUtils;
import cn.ponfee.disjob.common.util.Strings;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.registry.Discovery;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nullable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

/**
 * Discovery grouped server rest proxy
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
public final class DiscoveryGroupedServerRestProxy<T> {

    private static final ConcurrentMap<Method, Request> METHOD_REQUEST_CACHE = new ConcurrentHashMap<>();

    private final Constructor<T> constructor;
    private final T localServiceProvider;
    private final Predicate<String> localGroupMatcher;
    private final DiscoveryServerRestTemplate<?> template;
    private final String prefixPath;

    private DiscoveryGroupedServerRestProxy(Constructor<T> constructor,
                                            T localServiceProvider,
                                            Predicate<String> localGroupMatcher,
                                            DiscoveryServerRestTemplate<?> template,
                                            String prefixPath) {
        this.constructor = constructor;
        this.localServiceProvider = localServiceProvider;
        this.localGroupMatcher = localGroupMatcher;
        this.template = template;
        this.prefixPath = prefixPath;
    }

    public T group(String group) {
        try {
            return constructor.newInstance(new GroupedServerInvocationHandler(group));
        } catch (Exception e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * Creates grouped rpc service client proxy.
     *
     * @param interfaceCls         the interface class
     * @param localServiceProvider the localServiceProvider
     * @param localGroupMatcher    the localGroupMatcher
     * @param discoverServer       the discoverServer
     * @param restTemplate         the restTemplate
     * @param retry                the retry config
     * @param <T>                  interface type
     * @param <D>                  discovery server type
     * @return rpc service client proxy
     */
    public static <T, D extends Server> DiscoveryGroupedServerRestProxy<T> of(Class<T> interfaceCls,
                                                                              @Nullable T localServiceProvider,
                                                                              Predicate<String> localGroupMatcher,
                                                                              Discovery<D> discoverServer,
                                                                              RestTemplate restTemplate,
                                                                              RetryProperties retry) {
        Constructor<T> constructor = ProxyUtils.getProxyConstructor(interfaceCls);
        DiscoveryServerRestTemplate<D> template = new DiscoveryServerRestTemplate<>(discoverServer, restTemplate, retry);
        String prefixPath = getMappingPath(AnnotationUtils.findAnnotation(interfaceCls, RequestMapping.class));
        return new DiscoveryGroupedServerRestProxy<>(constructor, localServiceProvider, localGroupMatcher, template, prefixPath);
    }

    // ------------------------------------------------------------------------others

    private class GroupedServerInvocationHandler implements InvocationHandler {
        private final String group;

        private GroupedServerInvocationHandler(String group) {
            this.group = Objects.requireNonNull(group);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (localServiceProvider != null && localGroupMatcher.test(group)) {
                return method.invoke(localServiceProvider, args);
            } else {
                Request req = buildRequest(method, prefixPath);
                return template.execute(method, group, req.httpMethod, req.servletPath, args);
            }
        }
    }

    static Request buildRequest(final Method method, final String prefixPath) {
        //
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
            String servletPath = Strings.concatPath(prefixPath, suffixPath);
            return Arrays.stream(mapping.method())
                .filter(Objects::nonNull)
                .findAny()
                .map(Enum::name)
                .map(HttpMethod::valueOf)
                .map(httpMethod -> new Request(servletPath, httpMethod))
                .orElseThrow(() -> new IllegalStateException("Invalid http mapping method: " + key.toGenericString()));
        });
    }

    static class Request {
        final String servletPath;
        final HttpMethod httpMethod;

        private Request(String servletPath, HttpMethod httpMethod) {
            this.servletPath = servletPath;
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
