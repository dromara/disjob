package cn.ponfee.scheduler.registry;

import cn.ponfee.scheduler.common.util.Collects;
import cn.ponfee.scheduler.common.util.Files;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
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
 * <p>{Alias for annotation: @link AnnotatedElementUtils#findMergedAnnotation(AnnotatedElement, Class)}
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

    public static <T> T create(Class<T> interfaceType, DiscoveryRestTemplate<?> discoveryRestTemplate) {
        String prefixPath = parsePath(AnnotationUtils.findAnnotation(interfaceType, RequestMapping.class));
        InvocationHandler invocationHandler = new RestInvocationHandler(discoveryRestTemplate, prefixPath);
        return (T) Proxy.newProxyInstance(interfaceType.getClassLoader(), new Class<?>[]{interfaceType}, invocationHandler);
    }

    private static class RestInvocationHandler implements InvocationHandler {
        private static final Map<Method, String> PATH_CACHE = new ConcurrentHashMap<>();
        private static final String PLACE_HOLDER = new String();

        private final DiscoveryRestTemplate<?> discoveryRestTemplate;
        private final String prefixPath;

        private RestInvocationHandler(DiscoveryRestTemplate<?> discoveryRestTemplate, String prefixPath) {
            this.discoveryRestTemplate = discoveryRestTemplate;
            this.prefixPath = prefixPath;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String path = getPath(prefixPath, method);
            return discoveryRestTemplate.execute(path, method.getReturnType(), args);
        }

        private static String getPath(String prefixPath, Method method) {
            String path = PATH_CACHE.computeIfAbsent(method, key -> {
                String suffixPath = parsePath(AnnotatedElementUtils.findMergedAnnotation(key, RequestMapping.class));
                if (prefixPath.isEmpty()) {
                    return suffixPath.isEmpty() ? PLACE_HOLDER : suffixPath;
                } else {
                    return suffixPath.isEmpty() ? prefixPath : prefixPath + Files.UNIX_FOLDER_SEPARATOR + suffixPath;
                }
            });

            if (path == PLACE_HOLDER) {
                throw new UnsupportedOperationException("Method is illegal http api: " + method);
            } else {
                return path;
            }
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
        if (path.startsWith(Files.UNIX_FOLDER_SEPARATOR)) {
            path = path.substring(1);
        }
        if (path.endsWith(Files.UNIX_FOLDER_SEPARATOR)) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

}
