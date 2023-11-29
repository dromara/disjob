/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry;

import cn.ponfee.disjob.common.base.Symbol.Str;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.util.ExtendMethodHandles;
import cn.ponfee.disjob.common.util.Files;
import cn.ponfee.disjob.common.util.ProxyUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

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

    private static final ThreadLocal<String> GROUPED_SERVER_THREADLOCAL = new ThreadLocal<>();

    public interface GroupedServer {
        default void group(String groupName) {
            GROUPED_SERVER_THREADLOCAL.set(groupName);
        }
    }

    public static <T> T create(boolean grouped, Class<T> interfaceType, DiscoveryRestTemplate<?> discoveryRestTemplate) {
        Class<?>[] interfaces = grouped ? new Class[]{interfaceType, GroupedServer.class} : new Class[]{interfaceType};
        String prefixPath = parsePath(AnnotationUtils.findAnnotation(interfaceType, RequestMapping.class));
        InvocationHandler invocationHandler = new RestInvocationHandler(discoveryRestTemplate, prefixPath);
        return ProxyUtils.create(invocationHandler, interfaces);
    }

    private static class RestInvocationHandler implements InvocationHandler {
        private static final Map<Method, Request> REQUEST_CACHE = new ConcurrentHashMap<>();
        private static final Map<Method, MethodHandle> METHOD_HANDLE_CACHE = new ConcurrentHashMap<>();

        private final DiscoveryRestTemplate<?> discoveryRestTemplate;
        private final String prefixPath;

        private RestInvocationHandler(DiscoveryRestTemplate<?> discoveryRestTemplate, String prefixPath) {
            this.discoveryRestTemplate = discoveryRestTemplate;
            this.prefixPath = prefixPath;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Request request = buildRequest(prefixPath, method);
            if (proxy instanceof GroupedServer) {
                if (request == null) {
                    //assert method.equals(GroupedServer.class.getDeclaredMethod("group", String.class));
                    MethodHandle methodHandle = METHOD_HANDLE_CACHE.computeIfAbsent(
                        method,
                        key -> ExtendMethodHandles.getSpecialMethodHandle(method).bindTo(proxy)
                    );
                    return methodHandle.invokeWithArguments(args);
                } else {
                    String group = GROUPED_SERVER_THREADLOCAL.get();
                    try {
                        return discoveryRestTemplate.execute(group, request.path, request.httpMethod, method.getGenericReturnType(), args);
                    } finally {
                        GROUPED_SERVER_THREADLOCAL.remove();
                    }
                }
            } else {
                Objects.requireNonNull(request, () -> "Invalid http request method: " + method.toGenericString());
                return discoveryRestTemplate.execute(null, request.path, request.httpMethod, method.getGenericReturnType(), args);
            }
        }

        private static Request buildRequest(String prefixPath, Method method) {
            return REQUEST_CACHE.computeIfAbsent(method, key -> {
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
    }

    private static class Request {
        private final String path;
        private final HttpMethod httpMethod;

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
    private static String parsePath(RequestMapping mapping) {
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
