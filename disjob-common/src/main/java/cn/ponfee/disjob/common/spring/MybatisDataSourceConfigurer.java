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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.boot.autoconfigure.SpringBootVFS;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <pre>
 * Enable mybatis dataSource
 *
 * Bean顺序：
 *   1）BeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry：所有的常规bean定义都将被加载，但还没有任何bean被实例化，这允许在下一个后处理阶段开始之前添加更多的bean定义。
 *   2）BeanFactoryPostProcessor#postProcessBeanFactory：所有bean定义都将被加载，但还没有任何bean被实例化。这允许重写或添加属性，甚至可以添加到急于初始化的bean中。
 *   3）constructor：调用Bean构造函数实例化Bean
 *   4）BeanPostProcessor#postProcessBeforeInitialization：Bean实例化之后，属性设置之前
 *   5）afterPropertiesSet：Bean属性设置
 *   6）init-method：初始化方法对Bean做修改
 *   7）BeanPostProcessor#postProcessAfterInitialization：Bean实例化之后且属性已经设置，然后可以在Initialization之后对Bean做修改
 * </pre>
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(MybatisDataSourceConfigurer.MybatisDataSourceRegistrar.class)
public @interface MybatisDataSourceConfigurer {

    String DATA_SOURCE_NAME_SUFFIX               = "DataSource";
    String SQL_SESSION_FACTORY_NAME_SUFFIX       = "SqlSessionFactory";
    String SQL_SESSION_TEMPLATE_NAME_SUFFIX      = "SqlSessionTemplate";
    String TX_MANAGER_NAME_SUFFIX                = "TransactionManager";
    String TX_TEMPLATE_NAME_SUFFIX               = "TransactionTemplate";
    String JDBC_TEMPLATE_NAME_SUFFIX             = "JdbcTemplate";
    String MAPPER_SCANNER_CONFIGURER_NAME_SUFFIX = "MapperScannerConfigurer";

    String dataSourceName();

    String[] mapperLocations() default {};

    String[] basePackages() default {};

    Class<?>[] basePackageClasses() default {};

    boolean mapUnderscoreToCamelCase() default true;

    int defaultFetchSize() default 100;

    int defaultStatementTimeout() default 25;

    // ----------------------------------------------------------------------------------------class defined

    class MybatisDataSourceRegistrar implements /*EnvironmentAware,*/ ImportBeanDefinitionRegistrar {

        private final Environment environment;

        MybatisDataSourceRegistrar(Environment environment) {
            this.environment = environment;
        }

        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            AnnotationAttributes attrs = SpringUtils.getAnnotationAttributes(importingClassMetadata, MybatisDataSourceConfigurer.class);
            if (attrs == null) {
                return;
            }

            String dataSourceName = attrs.getString("dataSourceName");
            Assert.hasText(dataSourceName, "DataSource name cannot be empty.");
            String dataSourceConfigPrefixKey = dataSourceName + ".datasource";
            String jdbcUrl = environment.getProperty(dataSourceConfigPrefixKey + ".jdbc-url");
            if (StringUtils.isBlank(jdbcUrl)) {
                return;
            }

            // MapperScannerConfigurer bean definition
            BeanDefinitionBuilder mapperScannerConfigurerBdb = BeanDefinitionBuilder.genericBeanDefinition(MapperScannerConfigurer.class);
            mapperScannerConfigurerBdb.addPropertyValue("processPropertyPlaceHolders", true);
            mapperScannerConfigurerBdb.addPropertyValue("basePackage", String.join(",", resolveBasePackages(importingClassMetadata, attrs)));
            mapperScannerConfigurerBdb.addPropertyValue("sqlSessionTemplateBeanName", dataSourceName + SQL_SESSION_TEMPLATE_NAME_SUFFIX);
            mapperScannerConfigurerBdb.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            registry.registerBeanDefinition(dataSourceName + MAPPER_SCANNER_CONFIGURER_NAME_SUFFIX, mapperScannerConfigurerBdb.getBeanDefinition());

            // DataSource bean definition
            BeanDefinitionBuilder dataSourceBdb = BeanDefinitionBuilder.genericBeanDefinition(DataSource.class);
            dataSourceBdb.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            AbstractBeanDefinition dataSourceBd = dataSourceBdb.getBeanDefinition();

            dataSourceBd.setInstanceSupplier(() -> {
                Binder binder = Binder.get(environment);
                DataSource dataSource = DataSourceBuilder.create().build();
                binder.bind(dataSourceConfigPrefixKey, Bindable.ofInstance(dataSource));
                return dataSource;
            });
            registry.registerBeanDefinition(dataSourceName + DATA_SOURCE_NAME_SUFFIX, dataSourceBd);

            // SqlSessionFactoryBean bean definition
            BeanDefinitionBuilder sqlSessionFactoryBeanBdb = BeanDefinitionBuilder.genericBeanDefinition(SqlSessionFactoryBean.class);
            sqlSessionFactoryBeanBdb.addPropertyReference("dataSource", dataSourceName + DATA_SOURCE_NAME_SUFFIX);
            sqlSessionFactoryBeanBdb.addPropertyValue("configuration", createMybatisConfiguration(attrs));
            sqlSessionFactoryBeanBdb.addPropertyValue("mapperLocations", resolveMapperLocations(importingClassMetadata, attrs));
            sqlSessionFactoryBeanBdb.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            registry.registerBeanDefinition(dataSourceName + SQL_SESSION_FACTORY_NAME_SUFFIX, sqlSessionFactoryBeanBdb.getBeanDefinition());

            // SqlSessionTemplate bean definition
            BeanDefinitionBuilder sqlSessionTemplateBdb = BeanDefinitionBuilder.genericBeanDefinition(SqlSessionTemplate.class);
            sqlSessionTemplateBdb.addConstructorArgReference(dataSourceName + SQL_SESSION_FACTORY_NAME_SUFFIX);
            sqlSessionTemplateBdb.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            registry.registerBeanDefinition(dataSourceName + SQL_SESSION_TEMPLATE_NAME_SUFFIX, sqlSessionTemplateBdb.getBeanDefinition());

            // JdbcTemplate bean definition
            BeanDefinitionBuilder jdbcTemplateBdb = BeanDefinitionBuilder.genericBeanDefinition(JdbcTemplate.class);
            jdbcTemplateBdb.addConstructorArgReference(dataSourceName + DATA_SOURCE_NAME_SUFFIX);
            jdbcTemplateBdb.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            registry.registerBeanDefinition(dataSourceName + JDBC_TEMPLATE_NAME_SUFFIX, jdbcTemplateBdb.getBeanDefinition());

            // DataSourceTransactionManager bean definition
            BeanDefinitionBuilder dataSourceTransactionManagerBdb = BeanDefinitionBuilder.genericBeanDefinition(DataSourceTransactionManager.class);
            dataSourceTransactionManagerBdb.addConstructorArgReference(dataSourceName + DATA_SOURCE_NAME_SUFFIX);
            dataSourceTransactionManagerBdb.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            registry.registerBeanDefinition(dataSourceName + TX_MANAGER_NAME_SUFFIX, dataSourceTransactionManagerBdb.getBeanDefinition());

            // TransactionTemplate bean definition
            BeanDefinitionBuilder transactionTemplateBdb = BeanDefinitionBuilder.genericBeanDefinition(TransactionTemplate.class);
            transactionTemplateBdb.addConstructorArgReference(dataSourceName + TX_MANAGER_NAME_SUFFIX);
            transactionTemplateBdb.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            registry.registerBeanDefinition(dataSourceName + TX_TEMPLATE_NAME_SUFFIX, transactionTemplateBdb.getBeanDefinition());
        }

        private static List<String> resolveBasePackages(AnnotationMetadata importingClassMetadata, AnnotationAttributes attrs) {
            List<String> basePackages = new ArrayList<>();
            Arrays.stream(attrs.getStringArray("basePackages")).filter(StringUtils::isNotBlank).forEach(basePackages::add);
            Arrays.stream(attrs.getClassArray("basePackageClasses")).map(ClassUtils::getPackageName).forEach(basePackages::add);
            if (basePackages.isEmpty()) {
                basePackages.add(ClassUtils.getPackageName(importingClassMetadata.getClassName()));
            }
            return basePackages;
        }

        private static Resource[] resolveMapperLocations(AnnotationMetadata importingClassMetadata, AnnotationAttributes attrs) {
            String[] mapperLocations = attrs.getStringArray("mapperLocations");
            if (ArrayUtils.isEmpty(mapperLocations)) {
                mapperLocations = resolveBasePackages(importingClassMetadata, attrs)
                    .stream()
                    .filter(StringUtils::isNotBlank)
                    .map(e -> "classpath*:" + e.replace('.', '/') + "/**/*.xml")
                    .toArray(String[]::new);
            }

            try {
                List<Resource> resources = new ArrayList<>();
                for (String mapperLocation : mapperLocations) {
                    resources.addAll(Arrays.asList(new PathMatchingResourcePatternResolver().getResources(mapperLocation)));
                }
                return resources.toArray(new Resource[0]);
            } catch (IOException e) {
                String locations = Arrays.toString(mapperLocations);
                throw new BeanInstantiationException(SqlSessionFactory.class, "Load mybatis mapper locations error: " + locations, e);
            }
        }

        private static Configuration createMybatisConfiguration(AnnotationAttributes attrs) {
            VFS.addImplClass(SpringBootVFS.class);

            Configuration configuration = new Configuration();
            // 下划线转驼峰：默认false
            configuration.setMapUnderscoreToCamelCase(attrs.getBoolean("mapUnderscoreToCamelCase"));
            // 为驱动的结果集获取数量（fetchSize）设置一个建议值，此参数只可以在查询设置中被覆盖：默认null
            configuration.setDefaultFetchSize(attrs.getNumber("defaultFetchSize"));
            // 超时时间，它决定数据库驱动等待数据库响应的秒数：默认null
            configuration.setDefaultStatementTimeout(attrs.getNumber("defaultStatementTimeout"));
            return configuration;
        }
    }

}
