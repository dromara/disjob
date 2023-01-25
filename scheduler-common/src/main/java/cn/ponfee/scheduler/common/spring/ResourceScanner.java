/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.spring;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Map.Entry;

import static org.springframework.core.io.support.ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX;

/**
 * <pre>
 *   用法：
 *   new ResourceScanner("∕**∕").scan4text("*.properties")
 *   new ResourceScanner("∕**∕").scan4text("*.class");
 *   new ResourceScanner("∕").scan4text("*.xml");
 *   new ResourceScanner("∕**∕").scan4text("*.xml")
 *   
 *   new ResourceScanner("code.ponfee").scan4class();
 *   new ResourceScanner("code.ponfee").scan4class(new Class[] { Service.class });
 *   new ResourceScanner(ClassUtils.getPackagePath(XXX.class)).scan4text("abc.txt");
 * </pre>
 * 
 * 资源扫描
 * 
 * @author Ponfee
 * @see org.springframework.context.annotation.ClassPathBeanDefinitionScanner
 */
public class ResourceScanner {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceScanner.class);

    private final List<String> scanPaths = new LinkedList<>();

    /**
     * @param paths 扫描路径
     */
    public ResourceScanner(String... paths) {
        if (paths == null || paths.length == 0) {
            paths = new String[] { "*" };
        }

        Collections.addAll(this.scanPaths, paths);
    }

    /**
     * 类扫描
     * @return
     */
    @SuppressWarnings("unchecked")
    public Set<Class<?>> scan4class() {
        return scan4class(new Class[0]);
    }

    /**
     * 类扫描
     * @param annotations 包含指定注解的类
     * @return
     */
    @SuppressWarnings("unchecked")
    public Set<Class<?>> scan4class(Class<? extends Annotation>... annotations) {
        if (this.scanPaths.isEmpty()) {
            return Collections.emptySet();
        }

        List<TypeFilter> typeFilters = new LinkedList<>();
        if (annotations != null) {
            for (Class<? extends Annotation> annotation : annotations) {
                typeFilters.add(new AnnotationTypeFilter(annotation, false));
            }
        }

        Set<Class<?>> result = new HashSet<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            for (String packageName : this.scanPaths) {
                packageName = packageName.replace('.', '/');
                Resource[] resources = resolver.getResources(
                    CLASSPATH_ALL_URL_PREFIX + packageName + "/**/*.class"
                );
                MetadataReaderFactory mrf = new CachingMetadataReaderFactory(resolver);
                for (Resource resource : resources) {
                    if (!resource.isReadable()) {
                        continue;
                    }

                    MetadataReader reader = mrf.getMetadataReader(resource);
                    if (!this.matchesFilter(reader, typeFilters, mrf)) {
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
            throw new RuntimeException(e);
        }
    }

    /**
     * Scan as byte array
     * 
     * @return Map<String, byte[]>
     */
    public Map<String, byte[]> scan4binary() {
        return scan4binary("*");
    }

    /**
     * Scan as byte array
     * 
     * @param wildcard 通配符
     * @return a result of Map<String, byte[]>
     */
    public Map<String, byte[]> scan4binary(String wildcard) {
        if (wildcard == null) {
            wildcard = "*";
        }

        Map<String, byte[]> result = new HashMap<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            for (String path : this.scanPaths) {
                Resource[] resources = resolver.getResources(CLASSPATH_ALL_URL_PREFIX + path + wildcard);
                for (Resource resource : resources) {
                    if (!resource.isReadable()) {
                        continue;
                    }
                    try (InputStream in = resource.getInputStream()) {
                        result.put(resource.getFilename(), IOUtils.toByteArray(in));
                    } catch (IOException e) {
                        LOG.error("scan binary error", e);
                    }
                }
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 文本扫描
     * 
     * @return
     */
    public Map<String, String> scan4text() {
        return scan4text(null, Charset.defaultCharset());
    }

    /**
     * 文本扫描
     * 
     * @param wildcard
     * @return
     */
    public Map<String, String> scan4text(String wildcard) {
        return scan4text(wildcard, Charset.defaultCharset());
    }

    /**
     * 文本扫描
     * 
     * @param wildcard
     * @param charset
     * @return
     */
    public Map<String, String> scan4text(String wildcard, Charset charset) {
        Map<String, String> result = new HashMap<>();
        for (Entry<String, byte[]> entry : scan4binary(wildcard).entrySet()) {
            result.put(entry.getKey(), new String(entry.getValue(), charset));
        }
        return result;
    }

    // --------------------------------------------------------------------------private methods
    /**
     * 检查当前扫描到的Bean含有任何一个指定的注解标记
     * 
     * @param reader the MetadataReader
     * @param typeFilters the List<TypeFilter>
     * @param factory the MetadataReaderFactory
     * @return {@code true} means matched
     * @throws IOException if occur IOException
     */
    private boolean matchesFilter(MetadataReader reader, List<TypeFilter> typeFilters,
                                  MetadataReaderFactory factory) throws IOException {
        if (typeFilters.isEmpty()) {
            return true;
        }

        for (TypeFilter filter : typeFilters) {
            if (filter.match(reader, factory)) {
                return true;
            }
        }
        return false;
    }

}
