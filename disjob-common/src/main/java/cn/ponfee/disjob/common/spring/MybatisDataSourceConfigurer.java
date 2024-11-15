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

import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.spring.MybatisDataSourceConfigurer.MybatisDataSourceRegistrar;
import cn.ponfee.disjob.common.util.Strings;
import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Configure mybatis dataSource
 *
 * Bean顺序：
 *   1）BeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry：所有的常规bean定义都将被加载，但还没有任何bean被实例化，这允许在下一个后处理阶段开始之前添加更多的bean定义。
 *   2）BeanFactoryPostProcessor#postProcessBeanFactory：所有bean定义都将被加载，但还没有任何bean被实例化。这允许重写或添加属性，甚至可以添加到急于初始化的bean中。
 *   3）constructor：调用Bean构造函数实例化Bean
 *   4）BeanPostProcessor#postProcessBeforeInitialization：Bean实例化之后，属性设置之前
 *   5）afterPropertiesSet：Bean属性设置
 *   6）init-method：初始化方法对Bean做修改
 *   7）BeanPostProcessor#postProcessAfterInitialization：Bean实例化之后且属性已经设置，然后可以在Initialization之后对Bean做修改
 *
 * 不用显示声明`@EnableTransactionManagement`
 *   1）`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`配置了`TransactionAutoConfiguration`
 * </pre>
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(MybatisDataSourceRegistrar.class)
public @interface MybatisDataSourceConfigurer {

    String DATA_SOURCE_NAME_SUFFIX               = "DataSource";
    String SQL_SESSION_FACTORY_NAME_SUFFIX       = "SqlSessionFactory";
    String SQL_SESSION_TEMPLATE_NAME_SUFFIX      = "SqlSessionTemplate";
    String TX_MANAGER_NAME_SUFFIX                = "TransactionManager";
    String TX_TEMPLATE_NAME_SUFFIX               = "TransactionTemplate";
    String JDBC_TEMPLATE_NAME_SUFFIX             = "JdbcTemplate";
    String MAPPER_SCANNER_CONFIGURER_NAME_SUFFIX = "MapperScannerConfigurer";

    String dataSourceName() default "";

    String[] mapperLocations() default {};

    String[] basePackages() default {};

    Class<?>[] basePackageClasses() default {};

    boolean mapUnderscoreToCamelCase() default true;

    int defaultFetchSize() default 100;

    int defaultStatementTimeout() default 25;

    boolean primary() default false;

    // ----------------------------------------------------------------------------------------class defined

    class MybatisDataSourceRegistrar implements /*EnvironmentAware,*/ ImportBeanDefinitionRegistrar {
        private static final Logger LOG = LoggerFactory.getLogger(MybatisDataSourceRegistrar.class);
        private static final String KEY_PREFIX = "disjob.datasource.";

        private final Environment environment;

        MybatisDataSourceRegistrar(Environment environment) {
            this.environment = environment;
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            MybatisDataSourceConfigurer config = SpringUtils.parseAnnotation(MybatisDataSourceConfigurer.class, importingClassMetadata);
            if (config == null) {
                throw new IllegalArgumentException("MybatisDataSourceConfigurer cannot be null.");
            }

            List<String> basePackages = resolveBasePackages(config, importingClassMetadata);
            Assert.notEmpty(basePackages, "Base package is empty.");

            String dataSourceName = config.dataSourceName();
            if (StringUtils.isBlank(dataSourceName)) {
                dataSourceName = resolvePackageDatasourceName(basePackages.get(0));
            }
            Assert.hasText(dataSourceName, "DataSource name cannot be empty.");

            String dataSourceConfigPrefixKey = KEY_PREFIX + dataSourceName;
            String jdbcUrl = environment.getProperty(dataSourceConfigPrefixKey + ".jdbc-url");
            if (StringUtils.isBlank(jdbcUrl)) {
                LOG.warn("Datasource '{}' not configured jdbc-url value.", dataSourceName);
                return;
            }

            SpringUtils.addPropertyIfAbsent(environment, dataSourceConfigPrefixKey + ".pool-name", dataSourceName);
            boolean primary = config.primary();

            // MapperScannerConfigurer bean definition
            BeanDefinitionBuilder mapperScannerConfigurerBdb = newBeanDefinitionBuilder(MapperScannerConfigurer.class, primary);
            mapperScannerConfigurerBdb.addPropertyValue("processPropertyPlaceHolders", true);
            mapperScannerConfigurerBdb.addPropertyValue("basePackage", String.join(",", basePackages));
            mapperScannerConfigurerBdb.addPropertyValue("sqlSessionTemplateBeanName", dataSourceName + SQL_SESSION_TEMPLATE_NAME_SUFFIX);
            registry.registerBeanDefinition(dataSourceName + MAPPER_SCANNER_CONFIGURER_NAME_SUFFIX, mapperScannerConfigurerBdb.getBeanDefinition());

            // DataSource bean definition
            BeanDefinitionBuilder dataSourceBdb = newBeanDefinitionBuilder(DataSource.class, primary);
            AbstractBeanDefinition dataSourceBd = dataSourceBdb.getBeanDefinition();
            dataSourceBd.setInstanceSupplier(() -> {
                Binder binder = Binder.get(environment);
                DataSource dataSource = DataSourceBuilder.create().build();
                binder.bind(dataSourceConfigPrefixKey, Bindable.ofInstance(dataSource));
                return dataSource;
            });
            registry.registerBeanDefinition(dataSourceName + DATA_SOURCE_NAME_SUFFIX, dataSourceBd);

            // SqlSessionFactoryBean bean definition
            BeanDefinitionBuilder sqlSessionFactoryBeanBdb = newBeanDefinitionBuilder(SqlSessionFactoryBean.class, primary);
            sqlSessionFactoryBeanBdb.addPropertyReference("dataSource", dataSourceName + DATA_SOURCE_NAME_SUFFIX);
            sqlSessionFactoryBeanBdb.addPropertyValue("configuration", createMybatisConfiguration(config));
            sqlSessionFactoryBeanBdb.addPropertyValue("mapperLocations", resolveMapperLocations(config, basePackages));
            registry.registerBeanDefinition(dataSourceName + SQL_SESSION_FACTORY_NAME_SUFFIX, sqlSessionFactoryBeanBdb.getBeanDefinition());

            // SqlSessionTemplate bean definition
            BeanDefinitionBuilder sqlSessionTemplateBdb = newBeanDefinitionBuilder(SqlSessionTemplate.class, primary);
            sqlSessionTemplateBdb.addConstructorArgReference(dataSourceName + SQL_SESSION_FACTORY_NAME_SUFFIX);
            registry.registerBeanDefinition(dataSourceName + SQL_SESSION_TEMPLATE_NAME_SUFFIX, sqlSessionTemplateBdb.getBeanDefinition());

            // JdbcTemplate bean definition
            BeanDefinitionBuilder jdbcTemplateBdb = newBeanDefinitionBuilder(JdbcTemplate.class, primary);
            jdbcTemplateBdb.addConstructorArgReference(dataSourceName + DATA_SOURCE_NAME_SUFFIX);
            registry.registerBeanDefinition(dataSourceName + JDBC_TEMPLATE_NAME_SUFFIX, jdbcTemplateBdb.getBeanDefinition());

            // DataSourceTransactionManager bean definition
            BeanDefinitionBuilder dataSourceTransactionManagerBdb = newBeanDefinitionBuilder(DataSourceTransactionManager.class, primary);
            dataSourceTransactionManagerBdb.addConstructorArgReference(dataSourceName + DATA_SOURCE_NAME_SUFFIX);
            registry.registerBeanDefinition(dataSourceName + TX_MANAGER_NAME_SUFFIX, dataSourceTransactionManagerBdb.getBeanDefinition());

            // TransactionTemplate bean definition
            BeanDefinitionBuilder transactionTemplateBdb = newBeanDefinitionBuilder(TransactionTemplate.class, primary);
            transactionTemplateBdb.addConstructorArgReference(dataSourceName + TX_MANAGER_NAME_SUFFIX);
            registry.registerBeanDefinition(dataSourceName + TX_TEMPLATE_NAME_SUFFIX, transactionTemplateBdb.getBeanDefinition());
            LOG.info("Datasource '{}' registered bean definition.", dataSourceName);
        }

        public static void checkPackageDatasourceName(Class<?> basePackageClass, String expectDsName) {
            String actualDsName = resolvePackageDatasourceName(ClassUtils.getPackageName(basePackageClass));
            if (!actualDsName.equals(expectDsName)) {
                throw new IllegalStateException("Invalid data source name: expect=" + expectDsName + ", actual=" + actualDsName);
            }
        }

        // ----------------------------------------------------------------------------------------private methods

        private BeanDefinitionBuilder newBeanDefinitionBuilder(Class<?> beanType, boolean primary) {
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(beanType);
            builder.setPrimary(primary);
            builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            return builder;
        }

        private static String resolvePackageDatasourceName(String packageName) {
            String packageLastName = Strings.substringAfterLast(packageName, ".");
            // Spring boot的配置属性名只能包含{"a-z", "0-9", "-"}，它们必须为小写字母，且必须以字母或数字开头。"-"仅用于格式化，即"foo-bar"和"foobar"被认为是等效的。
            // org.springframework.boot.context.properties.source.ConfigurationPropertyName.ElementsParser#isValidChar
            // "_" -> "-"：java包名不能包含"-"，所以在包名命名时使用"_"来代替"-"。到了读取spring配置属性名时，需要把包名中的"_"转为spring boot配置中合法的属性名"-"。
            return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, packageLastName);
        }

        private static List<String> resolveBasePackages(MybatisDataSourceConfigurer config, AnnotationMetadata importingClassMetadata) {
            List<String> basePackages = Collects.asArrayList(config.basePackages());
            Arrays.stream(config.basePackageClasses()).map(ClassUtils::getPackageName).forEach(basePackages::add);
            if (basePackages.isEmpty()) {
                basePackages.add(ClassUtils.getPackageName(importingClassMetadata.getClassName()));
            }
            return basePackages.stream().filter(StringUtils::isNotBlank).collect(ImmutableList.toImmutableList());
        }

        private static Resource[] resolveMapperLocations(MybatisDataSourceConfigurer config, List<String> basePackages) {
            String[] mapperLocations = config.mapperLocations();
            if (ArrayUtils.isEmpty(mapperLocations)) {
                mapperLocations = basePackages.stream().map(e -> "classpath*:" + e.replace('.', '/') + "/**/*.xml").toArray(String[]::new);
            }

            try {
                List<Resource> resources = new ArrayList<>();
                for (String mapperLocation : mapperLocations) {
                    resources.addAll(Arrays.asList(new PathMatchingResourcePatternResolver().getResources(mapperLocation)));
                }
                return resources.toArray(new Resource[0]);
            } catch (IOException e) {
                String msg = Arrays.toString(mapperLocations);
                throw new BeanInstantiationException(SqlSessionFactory.class, "Load mybatis mapper locations error: " + msg, e);
            }
        }

        private static Configuration createMybatisConfiguration(MybatisDataSourceConfigurer config) {
            VFS.addImplClass(SpringBootVFS.class);

            Configuration configuration = new Configuration();
            // 下划线转驼峰：默认false
            configuration.setMapUnderscoreToCamelCase(config.mapUnderscoreToCamelCase());
            // 为驱动的结果集获取数量（fetchSize）设置一个建议值，此参数只可以在查询设置中被覆盖：默认null
            configuration.setDefaultFetchSize(config.defaultFetchSize());
            // 超时时间，它决定数据库驱动等待数据库响应的秒数：默认null
            configuration.setDefaultStatementTimeout(config.defaultStatementTimeout());
            return configuration;
        }
    }

}
