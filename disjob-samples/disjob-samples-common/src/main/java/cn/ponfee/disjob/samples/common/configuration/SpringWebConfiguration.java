/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.samples.common.configuration;

import cn.ponfee.disjob.common.spring.LocalizedMethodArgumentResolver;
import cn.ponfee.disjob.common.spring.RestTemplateUtils;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.base.HttpProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Spring web mvc configuration
 *
 * @author Ponfee
 */
@Configuration
@ComponentScan("cn.ponfee.disjob.test.handler")
public class SpringWebConfiguration implements WebMvcConfigurer {

    private final ObjectMapper objectMapper;

    public SpringWebConfiguration(@Nullable ObjectMapper objectMapper) {
        if (objectMapper == null) {
            objectMapper = Jsons.createObjectMapper(JsonInclude.Include.NON_NULL);
        } else {
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            Jsons.configObjectMapper(objectMapper);
        }

        // 解决spring-boot-admin-server客户端注册进来时反序列化报错问题
        // Cannot construct instance of `de.codecentric.boot.admin.server.domain.values.Registration` (no Creators, like default constructor, exist): cannot deserialize from Object value (no delegate- or property-based Creator)
        //com.fasterxml.jackson.databind.module.SimpleModule simpleModule = new com.fasterxml.jackson.databind.module.SimpleModule();
        //simpleModule.addSerializer(de.codecentric.boot.admin.server.domain.values.Registration.class, com.fasterxml.jackson.databind.ser.std.ToStringSerializer.instance);
        //simpleModule.addDeserializer(de.codecentric.boot.admin.server.domain.values.Registration.class, new de.codecentric.boot.admin.server.utils.jackson.RegistrationDeserializer());
        //objectMapper.registerModule(simpleModule);

        this.objectMapper = objectMapper;
    }

    /**
     * For spring mvc request body parameter.
     *
     * @return MappingJackson2HttpMessageConverter
     */
    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
        messageConverter.setObjectMapper(objectMapper);
        RestTemplateUtils.extensionSupportedMediaTypes(messageConverter);
        return messageConverter;
    }

    @ConditionalOnBean(HttpProperties.class)
    @Bean
    public RestTemplate restTemplate(HttpProperties httpProperties) {
        httpProperties.check();
        return RestTemplateUtils.buildRestTemplate(
            httpProperties.getConnectTimeout(),
            httpProperties.getReadTimeout(),
            StandardCharsets.UTF_8,
            mappingJackson2HttpMessageConverter()
        );
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(mappingJackson2HttpMessageConverter());
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new LocalizedMethodArgumentResolver());
    }

}
