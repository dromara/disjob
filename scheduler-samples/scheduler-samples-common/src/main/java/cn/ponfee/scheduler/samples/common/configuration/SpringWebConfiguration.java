/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.samples.common.configuration;

import cn.ponfee.scheduler.common.spring.LocalizedMethodArgumentResolver;
import cn.ponfee.scheduler.common.spring.RestTemplateUtils;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.base.HttpProperties;
import cn.ponfee.scheduler.core.base.JobConstants;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
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

    /**
     * Object mapper
     *
     * @return ObjectMapper
     */
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Bean({"objectMapper", JobConstants.SPRING_BEAN_NAME_OBJECT_MAPPER})
    @Primary
    public ObjectMapper objectMapper() {
        return Jsons.createObjectMapper(JsonInclude.Include.NON_NULL);
    }

    /**
     * For spring mvc request body parameter.
     *
     * @return MappingJackson2HttpMessageConverter
     */
    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
        messageConverter.setObjectMapper(objectMapper());
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
        resolvers.add(new LocalizedMethodArgumentResolver(objectMapper()));
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
