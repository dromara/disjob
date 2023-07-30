package com.ruoyi.common.utils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.*;

/**
 * Mybatis支持*匹配扫描包
 *
 * @author ruoyi
 */
public class MybatisUtils {

    private static final String DEFAULT_RESOURCE_PATTERN = "/**/*.class";

    public static String resolveTypeAliasesPackage(String typeAliasesPackage) throws IOException {
        if (StringUtils.isBlank(typeAliasesPackage)) {
            return null;
        }

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resolver);
        Set<String> packages = new HashSet<>();

        for (String aliasesPackage : typeAliasesPackage.split(",")) {
            aliasesPackage = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + ClassUtils.convertClassNameToResourcePath(aliasesPackage.trim()) + DEFAULT_RESOURCE_PATTERN;
            Resource[] resources = resolver.getResources(aliasesPackage);
            if (ArrayUtils.isNotEmpty(resources)) {
                for (Resource resource : resources) {
                    if (resource.isReadable()) {
                        MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
                        packages.add(ClassUtils.getPackageName(metadataReader.getClassMetadata().getClassName()));
                    }
                }
            }
        }
        return String.join(",", packages);
    }

    public static Resource[] resolveMapperLocations(String[] mapperLocations) {
        if (ArrayUtils.isEmpty(mapperLocations)) {
            return null;
        }
        ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
        List<Resource> resources = new ArrayList<>();
        for (String mapperLocation : mapperLocations) {
            try {
                Resource[] mappers = resourceResolver.getResources(mapperLocation);
                resources.addAll(Arrays.asList(mappers));
            } catch (IOException ignored) {
                // ignored
            }
        }
        return resources.toArray(new Resource[0]);
    }

}