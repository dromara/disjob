/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.base;

import cn.ponfee.scheduler.common.util.ClassUtils;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.text.MessageFormat;

/**
 * Abstract Datasource Configuration.
 *
 * @author Ponfee
 */
public abstract class AbstractDataSourceConfig {

    public static final String DATA_SOURCE_NAME_SUFFIX          = "DataSource";
    public static final String SQL_SESSION_FACTORY_NAME_SUFFIX  = "SqlSessionFactory";
    public static final String SQL_SESSION_TEMPLATE_NAME_SUFFIX = "SqlSessionTemplate";
    public static final String TX_MANAGER_NAME_SUFFIX           = "TransactionManager";
    public static final String TX_TEMPLATE_NAME_SUFFIX          = "TransactionTemplate";
    public static final String JDBC_TEMPLATE_NAME_SUFFIX        = "JdbcTemplate";

    private final String mapperFileLocation;

    public AbstractDataSourceConfig() {
        String basePackage = ClassUtils.getPackagePath(getClass());
        // cn/ponfee/scheduler/supervisor/dao/xml/*.xml
        this.mapperFileLocation = MessageFormat.format("classpath*:{0}xml/*.xml", basePackage);
    }

    public AbstractDataSourceConfig(String mapperFileLocation) {
        this.mapperFileLocation = mapperFileLocation;
    }

    /**
     * Create datasource, for subclasses implementations
     *
     * @return db datasource
     */
    public abstract DataSource dataSource();

    protected final SqlSessionFactory createSqlSessionFactory() throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource());
        factoryBean.setConfigLocation(new ClassPathResource("mybatis-config.xml"));
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(mapperFileLocation));
        return factoryBean.getObject();
    }

}
