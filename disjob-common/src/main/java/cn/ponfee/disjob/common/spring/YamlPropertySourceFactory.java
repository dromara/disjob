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

import org.apache.commons.lang3.StringUtils;
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
 *
 *   @Value("${datasource.username}")
 *   private String username;
 *
 *   @Value("${datasource.password}")
 *   private String password;
 * }}</pre>
 *
 * @author Ponfee
 */
public class YamlPropertySourceFactory extends DefaultPropertySourceFactory {

    @SuppressWarnings({"SingleStatementInBlock", "ConstantConditions"})
    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
        String sourceName = name != null ? name : resource.getResource().getFilename();
        if (!resource.getResource().exists()) {
            return new PropertiesPropertySource(sourceName, new Properties());
        } else if (StringUtils.endsWithAny(sourceName, ".yml", ".yaml")) {
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
