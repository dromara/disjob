/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

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
 *   new ResourceScanner("*.xml").scan4binary()
 *   new ResourceScanner("/*.xml").scan4binary()
 *   new ResourceScanner("**∕*.xml").scan4binary()
 *   new ResourceScanner("/**∕*.xml").scan4binary()
 *   new ResourceScanner("/log4j2.xml.template").scan4binary()
 *   new ResourceScanner("log4j2.xml.template").scan4binary()
 *
 *   new ResourceScanner("/cn/ponfee/commons/jce/*.class").scan4binary()
 *   new ResourceScanner("/cn/ponfee/commons/jce/**∕*.class").scan4binary()
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

    private final List<String> locationPatterns;

    public ResourceScanner(String... locationPatterns) {
        if (ArrayUtils.isEmpty(locationPatterns)) {
            locationPatterns = new String[]{"*"};
        }
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
                for (Resource resource : resolver.getResources(CLASSPATH_ALL_URL_PREFIX + locationPattern)) {
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
    public Map<String, byte[]> scan4binary() {
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
                for (Resource resource : resolver.getResources(CLASSPATH_ALL_URL_PREFIX + locationPattern)) {
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
