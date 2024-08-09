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

package cn.ponfee.disjob.core.base;

import cn.ponfee.disjob.common.spring.RestTemplateUtils;
import cn.ponfee.disjob.common.spring.RpcControllerConfigurer;
import cn.ponfee.disjob.common.spring.SpringContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestTemplate;

/**
 * Basic DeferredImportSelector
 *
 * @author Ponfee
 */
public class BasicDeferredImportSelector implements DeferredImportSelector {

    @SuppressWarnings("NullableProblems")
    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[]{BasicDeferredConfiguration.class.getName()};
    }

    /**
     * 推迟实例化，以支持用户使用自定义的Bean替代
     */
    private static class BasicDeferredConfiguration {

        /**
         * 如果@ConditionalOnMissingBean注解没有指定参数，则默认以方法的返回类型判断，即容器中不存在类型为`SpringContextHolder`的实例才创建
         *
         * @return SpringContextHolder
         */
        @ConditionalOnMissingBean
        @Bean
        public SpringContextHolder springContextHolder() {
            return new SpringContextHolder();
        }

        @ConditionalOnMissingBean
        @Bean
        public RetryProperties retryProperties() {
            return new RetryProperties();
        }

        @ConditionalOnMissingBean
        @Bean
        public HttpProperties httpProperties() {
            return new HttpProperties();
        }

        @ConditionalOnMissingBean(name = JobConstants.SPRING_BEAN_NAME_REST_TEMPLATE)
        @Bean(JobConstants.SPRING_BEAN_NAME_REST_TEMPLATE)
        public RestTemplate restTemplate(HttpProperties http, @Nullable ObjectMapper objectMapper) {
            http.check();
            return RestTemplateUtils.create(http.getConnectTimeout(), http.getReadTimeout(), objectMapper);
        }

        @ConditionalOnMissingBean
        @Bean
        public RpcControllerConfigurer rpcControllerConfigurer() {
            return new RpcControllerConfigurer();
        }
    }

}
