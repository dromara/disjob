package cn.ponfee.scheduler.registry;

import cn.ponfee.scheduler.common.util.Collects;
import cn.ponfee.scheduler.common.util.Files;
import cn.ponfee.scheduler.common.util.ObjectUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Retry rest proxy
 *
 * @author Ponfee
 */
public class DiscoveryRestProxy {

    public static <T> T create(Class<T> interfaceType, DiscoveryRestTemplate<?> discoveryRestTemplate) {
        // public @interface RequestMapping { @AliasFor("path") String[] value() default {}; @AliasFor("value") String[] path() default {}; }
        String prefixPath = parsePath(AnnotationUtils.findAnnotation(interfaceType, RequestMapping.class));
        InvocationHandler invocationHandler = new RestInvocationHandler(discoveryRestTemplate, prefixPath);
        return (T) Proxy.newProxyInstance(interfaceType.getClassLoader(), new Class<?>[]{interfaceType}, invocationHandler);
    }

    private static class RestInvocationHandler implements InvocationHandler {
        private static final Map<Method, String> PATH_CACHE = new HashMap<>();
        private static final String PLACE_HOLDER = ObjectUtils.uuid32();

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
            String path;
            if ((path = PATH_CACHE.get(method)) == null) {
                synchronized (PATH_CACHE) {
                    if ((path = PATH_CACHE.get(method)) == null) {
                        String suffixPath = parsePath(AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class));
                        if (prefixPath.isEmpty()) {
                            path = suffixPath.isEmpty() ? PLACE_HOLDER : suffixPath;
                        } else {
                            path = suffixPath.isEmpty() ? prefixPath : prefixPath + Files.UNIX_FOLDER_SEPARATOR + suffixPath;
                        }
                        PATH_CACHE.put(method, path);
                    }
                }
            }
            if (path != PLACE_HOLDER) {
                return path;
            }
            throw new UnsupportedOperationException("Method is illegal http api: " + method);
        }
    }

    /**
     * <pre>{@code
     *   @RequestMapping(method = {RequestMethod.POST})
     *   public @interface PostMapping {
     *       @AliasFor(annotation = RequestMapping.class)
     *       String[] value() default {};
     *
     *       @AliasFor(annotation = RequestMapping.class)
     *       String[] path() default {};
     *   }
     * }</pre>
     *
     * @param mapping the request mapping
     * @return path
     */
    private static String parsePath(RequestMapping mapping) {
        String path;
        if (mapping == null || (path = Collects.get(mapping.path(), 0)) == null || path.length() == 0) {
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
