package cn.ponfee.scheduler.supervisor.config;

import cn.ponfee.scheduler.common.spring.LocalizedMethodArgumentResolver;
import cn.ponfee.scheduler.common.util.Jsons;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import static cn.ponfee.scheduler.core.base.JobConstants.*;

/**
 * Web configuration
 *
 * @author Ponfee
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(WebConfiguration.class);

    /**
     * Object mapper
     *
     * @return ObjectMapper
     */
    @Bean({"objectMapper", HTTP_OBJECT_MAPPER_SPRING_BEAN_NAME})
    @Order(Ordered.HIGHEST_PRECEDENCE)
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
        List<MediaType> mediaTypes = new ArrayList<>(messageConverter.getSupportedMediaTypes().size() + 1);
        mediaTypes.addAll(messageConverter.getSupportedMediaTypes());
        mediaTypes.add(MediaType.TEXT_PLAIN);
        messageConverter.setSupportedMediaTypes(mediaTypes);
        return messageConverter;
    }

    @Bean
    public RestTemplate restTemplate(@Value("${" + HTTP_CONNECT_TIMEOUT_KEY + ":2000}") int connectTimeout,
                                     @Value("${" + HTTP_READ_TIMEOUT_KEY + ":5000}") int readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        restTemplate.setMessageConverters(Arrays.asList(
            new ByteArrayHttpMessageConverter(),
            new StringHttpMessageConverter(StandardCharsets.UTF_8),
            new ResourceHttpMessageConverter(),
            new SourceHttpMessageConverter<>(),
            new FormHttpMessageConverter(),
            mappingJackson2HttpMessageConverter()
        ));
        return restTemplate;
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
            Enumeration<String> names = request.getHeaderNames();
            StringBuilder header = new StringBuilder();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                header.append(name).append("=").append(request.getHeader(name)).append(" & ");
            }
            if (header.length() > 0) {
                header.setLength(header.length() - 3);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug(
                    "\nRequest URL: {}\nRequest Method: {}\nRequest Headers: {}\nRequest Parameter: {}\n",
                    request.getRequestURL(), request.getMethod(), header, request.getParameterMap()
                );
            }
            return true;
        }

        @Override
        public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
            StringBuilder header = new StringBuilder();
            response.getHeaderNames().forEach(name -> header.append(name).append("=").append(request.getHeader(name)).append(" & "));
            if (header.length() > 0) {
                header.setLength(header.length() - 3);
            }
            LOG.debug("\nResponse Headers: {}\n", header);
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
            // noop
        }
    }

}
