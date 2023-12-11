/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.base;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.boot.autoconfigure.SpringBootVFS;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    private final String mybatisMapperFileLocation;

    protected AbstractDataSourceConfig() {
        this(-1);
    }

    protected AbstractDataSourceConfig(int wildcardLastIndex) {
        List<String> list = Arrays.stream(ClassUtils.getPackageName(getClass()).split("\\."))
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toList());
        String path;
        if (list.isEmpty()) {
            path = "";
        } else if (wildcardLastIndex == 0) {
            path = String.join("/", list) + "/**/";
        } else if (wildcardLastIndex < 0) {
            path = String.join("/", list) + "/";
        } else if (list.size() <= wildcardLastIndex) {
            path = "/**/" + String.join("/", list) + "/";
        } else {
            int pos = list.size() - wildcardLastIndex;
            path = String.join("/", list.subList(0, pos)) + "/**/" + String.join("/", list.subList(pos, list.size())) + "/";
        }

        // -1 => classpath*:cn/ponfee/disjob/supervisor/dao/xml/*.xml
        //  0 => classpath*:cn/ponfee/disjob/supervisor/dao/**/xml/*.xml
        //  1 => classpath*:cn/ponfee/disjob/supervisor/**/dao/xml/*.xml
        this.mybatisMapperFileLocation = MessageFormat.format("classpath*:{0}xml/*.xml", path);
    }

    protected AbstractDataSourceConfig(String mybatisMapperFileLocation) {
        this.mybatisMapperFileLocation = mybatisMapperFileLocation;
    }

    /**
     * Create datasource, for subclasses implementations
     *
     * @return db datasource
     */
    public abstract DataSource dataSource();

    protected final SqlSessionFactory createSqlSessionFactory() throws Exception {
        VFS.addImplClass(SpringBootVFS.class);
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource());
        factoryBean.setConfiguration(createMybatisConfiguration());
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(mybatisMapperFileLocation));
        return factoryBean.getObject();
    }

    private static Configuration createMybatisConfiguration() {
        Configuration configuration = new Configuration();
        // 下划线转驼峰：默认false
        configuration.setMapUnderscoreToCamelCase(true);
        // 为驱动的结果集获取数量（fetchSize）设置一个建议值，此参数只可以在查询设置中被覆盖：默认null
        configuration.setDefaultFetchSize(100);
        // 超时时间，它决定数据库驱动等待数据库响应的秒数：默认null
        configuration.setDefaultStatementTimeout(25);

        return configuration;
    }

}
