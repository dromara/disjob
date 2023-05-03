/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.spring;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;

import java.io.IOException;
import java.util.Properties;

/**
 * Spring yaml properties source factory, 
 * <p>for help use annotation {@link org.springframework.context.annotation.PropertySource}
 * 
 * <pre>{@code
 * @PropertySource(value = "classpath:xxx.yml", factory = YamlPropertySourceFactory.class)
 * public class DruidConfig {
 *   @Value("${datasource.jdbc-url}")
 *   private String jdbcUrl;
 *   @Value("${datasource.username}")
 *   private String username;
 *   @Value("${datasource.password}")
 *   private String password;
 * }
 * }</pre>
 * 
 * @author Ponfee
 */
public class YamlPropertySourceFactory extends DefaultPropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
        String sourceName = name != null ? name : resource.getResource().getFilename();
        if (!resource.getResource().exists()) {
            return new PropertiesPropertySource(sourceName, new Properties());
        } else if (sourceName.endsWith(".yml") || sourceName.endsWith(".yaml")) {
            //return new YamlPropertySourceLoader().load(sourceName, resource.getResource()).get(0);
            return new PropertiesPropertySource(sourceName, loadYml(resource.getResource()));
        } else {
            return super.createPropertySource(name, resource);
        }
    }

    public static Properties loadYml(Resource resource) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(resource);
        factory.afterPropertiesSet();
        return factory.getObject();
    }

}
