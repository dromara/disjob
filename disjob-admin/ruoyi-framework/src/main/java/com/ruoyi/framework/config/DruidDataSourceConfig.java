package com.ruoyi.framework.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import com.alibaba.druid.spring.boot.autoconfigure.properties.DruidStatProperties;
import com.alibaba.druid.util.Utils;
import com.ruoyi.common.enums.DataSourceType;
import com.ruoyi.common.utils.MybatisUtils;
import com.ruoyi.framework.config.properties.DruidProperties;
import com.ruoyi.framework.config.properties.SimpleJdbcProperties;
import com.ruoyi.framework.datasource.DynamicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.SpringBootVFS;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.servlet.*;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * druid 配置多数据源
 *
 * @author ruoyi
 * @author Ponfee
 */
@Configuration
@MapperScan({"com.ruoyi.**.mapper"})
public class DruidDataSourceConfig {

    private static final String DRUID_PREFIX_KEY = "spring.datasource.druid";

    private static final String RUOYI_DRUID_PROPERTIES = "ruoyiDruidProperties";
    private static final String RUOYI_MASTER_JDBC_PROPERTIES = "ruoyiMasterJdbcProperties";
    private static final String RUOYI_SLAVE_JDBC_PROPERTIES = "ruoyiSlaveJdbcProperties";

    private final Environment env;

    public DruidDataSourceConfig(Environment env) {
        this.env = env;
    }

    @ConfigurationProperties(DRUID_PREFIX_KEY)
    @Bean(RUOYI_DRUID_PROPERTIES)
    public DruidProperties ruoyiDruidProperties() {
        return new DruidProperties();
    }

    @ConfigurationProperties(DRUID_PREFIX_KEY + ".master")
    @Bean(RUOYI_MASTER_JDBC_PROPERTIES)
    public SimpleJdbcProperties ruoyiMasterJdbcProperties() {
        return new SimpleJdbcProperties();
    }

    @ConfigurationProperties(DRUID_PREFIX_KEY + ".slave")
    @Bean(RUOYI_SLAVE_JDBC_PROPERTIES)
    public SimpleJdbcProperties ruoyiSlaveJdbcProperties() {
        return new SimpleJdbcProperties();
    }

    @Bean
    @Primary
    public DynamicDataSource ruoyiDynamicDataSource(@Qualifier(RUOYI_DRUID_PROPERTIES) DruidProperties druidProperties,
                                                    @Qualifier(RUOYI_MASTER_JDBC_PROPERTIES) SimpleJdbcProperties masterJdbcProperties,
                                                    @Qualifier(RUOYI_SLAVE_JDBC_PROPERTIES) SimpleJdbcProperties slaveJdbcProperties) {
        Map<Object, Object> targetDataSources = new HashMap<>(4);
        DruidDataSource masterDataSource = createDruidDataSource(masterJdbcProperties, druidProperties);
        targetDataSources.put(DataSourceType.MASTER.name(), masterDataSource);
        if (StringUtils.isNotBlank(slaveJdbcProperties.getUrl())) {
            DruidDataSource slaveDataSource = createDruidDataSource(slaveJdbcProperties, druidProperties);
            targetDataSources.put(DataSourceType.SLAVE.name(), slaveDataSource);
        }
        return new DynamicDataSource(masterDataSource, targetDataSources);
    }

    @Bean
    @Primary
    public SqlSessionFactory ruoyiSqlSessionFactory(DataSource dataSource) throws Exception {
        String configLocation = env.getRequiredProperty("mybatis.configLocation");
        String mapperLocations = env.getRequiredProperty("mybatis.mapperLocations");
        String typeAliasesPackage = MybatisUtils.resolveTypeAliasesPackage(env.getProperty("mybatis.typeAliasesPackage"));
        VFS.addImplClass(SpringBootVFS.class);

        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        if (StringUtils.isNotBlank(typeAliasesPackage)) {
            sessionFactory.setTypeAliasesPackage(typeAliasesPackage);
        }
        sessionFactory.setMapperLocations(MybatisUtils.resolveMapperLocations(StringUtils.split(mapperLocations, ",")));
        sessionFactory.setConfigLocation(new DefaultResourceLoader().getResource(configLocation));
        return sessionFactory.getObject();
    }

    @Bean
    @Primary
    public SqlSessionTemplate ruoyiSqlSessionTemplate(SqlSessionFactory ruoyiSqlSessionFactory) {
        return new SqlSessionTemplate(ruoyiSqlSessionFactory);
    }

    @Bean
    @Primary
    public DataSourceTransactionManager ruoyiTransactionManager(DynamicDataSource ruoyiDynamicDataSource) {
        return new DataSourceTransactionManager(ruoyiDynamicDataSource);
    }

    @Bean
    @Primary
    public TransactionTemplate ruoyiTransactionTemplate(DataSourceTransactionManager ruoyiSqlSessionTemplate) {
        return new TransactionTemplate(ruoyiSqlSessionTemplate);
    }

    @Bean
    @Primary
    public JdbcTemplate ruoyiJdbcTemplate(DynamicDataSource ruoyiDynamicDataSource) {
        return new JdbcTemplate(ruoyiDynamicDataSource);
    }

    /**
     * 去除监控页面底部的广告
     */
    @Bean
    @ConditionalOnProperty(name = DRUID_PREFIX_KEY + "statViewServlet.enabled", havingValue = "true")
    public FilterRegistrationBean<Filter> druidFilterRegistrationBean(DruidStatProperties properties) {
        // 获取web监控页面的参数
        DruidStatProperties.StatViewServlet config = properties.getStatViewServlet();
        // 提取common.js的配置路径
        String pattern = config.getUrlPattern() != null ? config.getUrlPattern() : "/druid/*";
        String commonJsPattern = pattern.replaceAll("\\*", "js/common.js");
        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new DruidFilter("support/http/resources/js/common.js"));
        registrationBean.addUrlPatterns(commonJsPattern);
        return registrationBean;
    }

    // ---------------------------------------------------------------private class

    private static class DruidFilter implements Filter {
        private final String path;

        private DruidFilter(String path) {
            this.path = path;
        }

        @Override
        public void init(javax.servlet.FilterConfig filterConfig) {
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
            chain.doFilter(request, response);
            // 重置缓冲区，响应头不会被重置
            response.resetBuffer();
            // 获取common.js
            String text = Utils.readFromResource(path);
            // 正则替换banner, 除去底部的广告信息
            text = text.replaceAll("<a.*?banner\"></a><br/>", "");
            text = text.replaceAll("powered.*?shrek.wang</a>", "");
            response.getWriter().write(text);
        }

        @Override
        public void destroy() {
        }
    }

    private static DruidDataSource createDruidDataSource(SimpleJdbcProperties jdbcProperties, DruidProperties druidProperties) {
        DruidDataSource druidDataSource = DruidDataSourceBuilder.create().build();
        druidDataSource.setUrl(jdbcProperties.getUrl());
        druidDataSource.setUsername(jdbcProperties.getUsername());
        druidDataSource.setPassword(jdbcProperties.getPassword());
        druidProperties.configureDataSource(druidDataSource);
        return druidDataSource;
    }

}
