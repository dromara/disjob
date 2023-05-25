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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;

/**
 * Spring web mvc configuration
 *
 * @author Ponfee
 */
@Configuration
public class SpringWebConfiguration implements WebMvcConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(SpringWebConfiguration.class);

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
    public RestTemplate restTemplate(HttpProperties properties) {
        return RestTemplateUtils.buildRestTemplate(
            properties.getConnectTimeout(),
            properties.getReadTimeout(),
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

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CustomHandlerInterceptor()).addPathPatterns("/**");
    }

    public static class CustomHandlerInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            if (LOG.isDebugEnabled()) {
                Enumeration<String> names = request.getHeaderNames();
                StringBuilder header = new StringBuilder();
                while (names.hasMoreElements()) {
                    String name = names.nextElement();
                    header.append(name).append("=").append(request.getHeader(name)).append(" & ");
                }
                if (header.length() > 0) {
                    header.setLength(header.length() - 3);
                }

                LOG.debug(
                    "\nRequest URL: {}\nRequest Method: {}\nRequest Headers: {}\nRequest Parameter: {}\n",
                    request.getRequestURL(), request.getMethod(), header, request.getParameterMap()
                );
            }
            return true;
        }

        @Override
        public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
            if (LOG.isDebugEnabled()) {
                StringBuilder header = new StringBuilder();
                response.getHeaderNames().forEach(name -> header.append(name).append("=").append(request.getHeader(name)).append(" & "));
                if (header.length() > 0) {
                    header.setLength(header.length() - 3);
                }
                LOG.debug("\nResponse Headers: {}\n", header);
            }
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
            // No-op
        }
    }

}
