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

/**
 * Retry rest proxy
 *
 * @author Ponfee
 */
public class DiscoveryRestProxy {

    public static <T> T create(Class<T> interfaceType, DiscoveryRestTemplate<?> discoveryRestTemplate) {
        // public @interface RequestMapping { @AliasFor("path") String[] value() default {}; @AliasFor("value") String[] path() default {}; }
        String prefixPath = getPath(AnnotationUtils.findAnnotation(interfaceType, RequestMapping.class));
        InvocationHandler invocationHandler = new RestInvocationHandler(discoveryRestTemplate, prefixPath);
        return (T) Proxy.newProxyInstance(interfaceType.getClassLoader(), new Class<?>[]{interfaceType}, invocationHandler);
    }

    private static class RestInvocationHandler implements InvocationHandler {
        private final DiscoveryRestTemplate<?> discoveryRestTemplate;
        private final String prefixPath;
        private final boolean emptyPrefixPath;

        private RestInvocationHandler(DiscoveryRestTemplate<?> discoveryRestTemplate, String prefixPath) {
            this.discoveryRestTemplate = discoveryRestTemplate;
            this.prefixPath = prefixPath;
            this.emptyPrefixPath = StringUtils.isEmpty(prefixPath);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String suffixPath = getPath(AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class));
            if (emptyPrefixPath && StringUtils.isEmpty(suffixPath)) {
                throw new UnsupportedOperationException("Method is not http api: " + method);
            }

            String uri = prefixPath + Files.UNIX_FOLDER_SEPARATOR + suffixPath;
            return discoveryRestTemplate.execute(uri, method.getReturnType(), args);
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
    private static String getPath(RequestMapping mapping) {
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
