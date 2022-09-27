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

    private final String mapperFileLocation;

    public AbstractDataSourceConfig() {
        String basePackage = ClassUtils.getPackagePath(getClass());
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
