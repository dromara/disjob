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
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.ClassPathResource;
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

    private final String mapperFileLocation;

    public AbstractDataSourceConfig() {
        this(-1);
    }

    public AbstractDataSourceConfig(int wildcardLastIndex) {
        List<String> list = Arrays.stream(ClassUtils.getPackageName(getClass()).split("\\."))
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toList());
        String path;
        if (list.size() == 0) {
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
        this.mapperFileLocation = MessageFormat.format("classpath*:{0}xml/*.xml", path);
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
