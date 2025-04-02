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

package cn.ponfee.disjob.common.spring;

import cn.ponfee.disjob.common.util.ProxyUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.server.WebServer;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring utils
 *
 * @author Ponfee
 */
public final class SpringUtils {

    /**
     * Spring-boot web server port key
     */
    public static final String SPRING_BOOT_SERVER_PORT = "server.port";

    /**
     * Spring-boot web server servlet context-path key
     */
    public static final String SPRING_BOOT_CONTEXT_PATH = "server.servlet.context-path";

    public static Resource getResource(String resourceLocation) throws IOException {
        // return new DefaultResourceLoader().getResource(resourceLocation);
        URL url = ResourceUtils.getURL(resourceLocation);
        byte[] bytes = IOUtils.toByteArray(url);
        return new InputStreamResource(new ByteArrayInputStream(bytes));
    }

    public static int getActualWebServerPort(WebServerApplicationContext webServerApplicationContext) {
        Integer port = webServerApplicationContext.getEnvironment().getProperty(SPRING_BOOT_SERVER_PORT, Integer.class);
        if (port != null && port > 0) {
            return port;
        }

        // Ahead start web server for get actual port
        WebServer webServer = webServerApplicationContext.getWebServer();
        webServer.start();
        // port=null ->  default 8080(如果默认的8080端口被占用，则启动时会抛异常)
        // port=0    ->  random available port
        return webServer.getPort();
    }

    public static <T extends Annotation> T getAnnotation(HandlerMethod handlerMethod, Class<T> annotationType) {
        T annotation = handlerMethod.getMethodAnnotation(annotationType);
        return annotation != null ? annotation : handlerMethod.getBeanType().getAnnotation(annotationType);
    }

    public static <T extends Annotation> Set<String> getAnnotatedUrlPath(RequestMappingHandlerMapping mapping,
                                                                         Class<T> annotationType) {
        Set<String> urlPaths = new HashSet<>();
        mapping.getHandlerMethods().forEach((requestMappingInfo, handlerMethod) -> {
            if (getAnnotation(handlerMethod, annotationType) != null) {
                // requestMappingInfo#getPatternValues()会包含占位符路径：`/system/menu/add/{parentId}`
                urlPaths.addAll(requestMappingInfo.getDirectPaths());
            }
        });
        return urlPaths;
    }

    public static AnnotationAttributes getAnnotationAttributes(Class<? extends Annotation> type,
                                                               AnnotatedTypeMetadata metadata) {
        return AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(type.getName()));
    }

    public static <T extends Annotation> T parseAnnotation(Class<T> type, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(type.getName());
        if (attributes == null) {
            throw new IllegalArgumentException("Not found annotated type: " + type);
        }
        return parseAnnotation(type, attributes);
    }

    public static <T extends Annotation> T parseAnnotation(Class<T> type, Map<String, Object> attributes) {
        // `sun.reflect.annotation`包不可见，需要`add-exports`，因此用`ProxyUtils#create(Class,Map)`替换
        //return (T) AnnotationParser.annotationForMap(type, attributes == null ? Collections.emptyMap() : attributes);
        return ProxyUtils.create(type, attributes);
    }

    public static void addPropertySource(ConfigurableEnvironment env, String propertySourceName, Map<String, ?> map) {
        Assert.notEmpty(map, "Additional properties cannot be empty.");
        Properties properties = new Properties();
        properties.putAll(map);
        env.getPropertySources().addLast(new PropertiesPropertySource(propertySourceName, properties));
    }

    public static void addPropertySourceIfAbsent(ConfigurableEnvironment env, String propertySourceName, Map<String, ?> map) {
        map = map.entrySet()
            .stream()
            .filter(e -> !env.containsProperty(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!map.isEmpty()) {
            addPropertySource(env, propertySourceName, map);
        }
    }

}
