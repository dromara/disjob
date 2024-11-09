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

package com.ruoyi.framework.config;

import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.framework.shiro.web.CustomShiroFilterFactoryBean;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Anonymous permit config
 *
 * @author Ponfee
 */
@Configuration
class AnonymousPermitConfig {

    AnonymousPermitConfig(CustomShiroFilterFactoryBean customShiroFilterFactoryBean,
                          RequestMappingHandlerMapping requestMappingHandlerMapping) {
        Set<String> urlPaths = SpringUtils.findAnnotatedUrlPath(requestMappingHandlerMapping, Anonymous.class);
        if (CollectionUtils.isEmpty(urlPaths)) {
            return;
        }
        Map<String, String> filterChainDefinitionMap = customShiroFilterFactoryBean.getFilterChainDefinitionMap();
        if (filterChainDefinitionMap == null) {
            filterChainDefinitionMap = new HashMap<>();
            customShiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);
        }
        for (String urlPath : urlPaths) {
            filterChainDefinitionMap.put(urlPath, "anon");
        }
    }

}
