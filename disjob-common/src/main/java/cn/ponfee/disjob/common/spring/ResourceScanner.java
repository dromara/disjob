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

import cn.ponfee.disjob.common.exception.Throwables.ThrowingFunction;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.*;

import static org.springframework.core.io.support.ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX;

/**
 * <pre>
 *  资源扫描文件，用法：
 *   new ResourceScanner("/*.template").scan4text()
 *   new ResourceScanner("/**∕tika*.xml").scan4text()
 *
 *   // findAllClassPathResources：“/*” 等同 “*”，“/”开头会被截取path.substring(1)
 *   new ResourceScanner("*.xml").scan4bytes()
 *   new ResourceScanner("/*.xml").scan4bytes()
 *   new ResourceScanner("**∕*.xml").scan4bytes()
 *   new ResourceScanner("/**∕*.xml").scan4bytes()
 *   new ResourceScanner("log4j2.xml.template").scan4bytes()
 *   new ResourceScanner("/log4j2.xml.template").scan4bytes()
 *
 *   new ResourceScanner("/cn/ponfee/commons/jce/*.class").scan4bytes()
 *   new ResourceScanner("/cn/ponfee/commons/jce/**∕*.class").scan4bytes()
 *
 *   new ResourceScanner("/cn/ponfee/commons/base/**∕*.class").scan4class()
 *   new ResourceScanner("/cn/ponfee/commons/**∕*.class").scan4class(null, new Class[] {Service.class})
 *   new ResourceScanner("/cn/ponfee/commons/**∕*.class").scan4class(null, new Class[] {Component.class})
 *   new ResourceScanner("/cn/ponfee/commons/**∕*.class").scan4class(new Class[]{Tuple.class}, null)
 * </pre>
 *
 * @author Ponfee
 * @see org.springframework.context.annotation.ClassPathBeanDefinitionScanner
 */
public class ResourceScanner {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceScanner.class);

    /**
     * Prefix of resource schema.
     *
     * @see org.springframework.core.io.support.ResourcePatternResolver#CLASSPATH_ALL_URL_PREFIX
     * @see org.springframework.util.ResourceUtils#CLASSPATH_URL_PREFIX
     * @see org.springframework.util.ResourceUtils#FILE_URL_PREFIX
     * @see org.springframework.util.ResourceUtils#JAR_URL_PREFIX
     * @see org.springframework.util.ResourceUtils#WAR_URL_PREFIX
     */
    private final String urlPrefix;

    private final List<String> locationPatterns;

    public ResourceScanner(String... locationPatterns) {
        this(CLASSPATH_ALL_URL_PREFIX, locationPatterns);
    }

    public ResourceScanner(String urlPrefix, String[] locationPatterns) {
        if (ArrayUtils.isEmpty(locationPatterns)) {
            locationPatterns = new String[]{"*"};
        }
        this.urlPrefix = urlPrefix;
        this.locationPatterns = Arrays.asList(locationPatterns);
    }

    /**
     * 类扫描
     *
     * @return result of class set
     */
    public Set<Class<?>> scan4class() {
        return scan4class(null, null);
    }

    /**
     * 类扫描
     *
     * @param assignableTypes 扫描指定的子类
     * @param annotationTypes 扫描包含指定注解的类
     * @return result of class set
     */
    public Set<Class<?>> scan4class(Class<?>[] assignableTypes, Class<? extends Annotation>[] annotationTypes) {
        List<TypeFilter> typeFilters = new LinkedList<>();

        if (ArrayUtils.isNotEmpty(assignableTypes)) {
            Arrays.stream(assignableTypes).map(AssignableTypeFilter::new).forEach(typeFilters::add);
        }
        if (ArrayUtils.isNotEmpty(annotationTypes)) {
            // considerMetaAnnotations=true: @Service -> @Component
            Arrays.stream(annotationTypes).map(AnnotationTypeFilter::new).forEach(typeFilters::add);
        }

        Set<Class<?>> result = new HashSet<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        MetadataReaderFactory factory = new CachingMetadataReaderFactory(resolver);
        try {
            for (String locationPattern : this.locationPatterns) {
                for (Resource resource : resolver.getResources(urlPrefix + locationPattern)) {
                    if (!resource.isReadable()) {
                        continue;
                    }
                    MetadataReader reader = factory.getMetadataReader(resource);
                    if (!matches(typeFilters, reader, factory)) {
                        continue;
                    }
                    try {
                        result.add(Class.forName(reader.getClassMetadata().getClassName()));
                    } catch (Throwable e) {
                        LOG.error("Load class occur error.", e);
                    }
                }
            }
            return result;
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * Scan as byte array
     *
     * @return type of Map<String, byte[]> result
     */
    public Map<String, byte[]> scan4bytes() {
        return scan(IOUtils::toByteArray);
    }

    /**
     * Scan as string
     *
     * @return type of Map<String, String> result
     */
    public Map<String, String> scan4text() {
        return scan4text(Charset.defaultCharset());
    }

    /**
     * Scan as string
     *
     * @param charset the charset
     * @return type of Map<String, String> result
     */
    public Map<String, String> scan4text(Charset charset) {
        return scan(e -> IOUtils.toString(e, charset));
    }

    // --------------------------------------------------------------------------private methods

    private <T> Map<String, T> scan(ThrowingFunction<InputStream, T, ?> mapper) {
        Map<String, T> result = new HashMap<>(16);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            for (String locationPattern : locationPatterns) {
                for (Resource resource : resolver.getResources(urlPrefix + locationPattern)) {
                    if (!resource.isReadable()) {
                        continue;
                    }
                    try (InputStream input = resource.getInputStream()) {
                        result.put(resource.getFilename(), mapper.apply(input));
                    } catch (Throwable e) {
                        LOG.error("Resource scan location pattern failed: " + locationPattern, e);
                    }
                }
            }
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
        return result;
    }

    private static boolean matches(List<TypeFilter> filters, MetadataReader reader, MetadataReaderFactory factory) throws IOException {
        if (filters.isEmpty()) {
            return true;
        }
        for (TypeFilter filter : filters) {
            if (filter.match(reader, factory)) {
                return true;
            }
        }
        return false;
    }

}
