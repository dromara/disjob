/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.spring;

import cn.ponfee.scheduler.common.util.Collects;
import cn.ponfee.scheduler.common.util.Jsons;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;
import org.springframework.core.NamedThreadLocal;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Array;
import java.net.URI;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.springframework.http.HttpMethod.*;

/**
 * Spring rest template utility.
 *
 * @author Ponfee
 */
public class RestTemplateUtils {
    public static final Set<HttpMethod> QUERY_PARAMS = ImmutableSet.of(GET, DELETE, HEAD, OPTIONS);

    public static MappingJackson2HttpMessageConverter buildJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
        messageConverter.setObjectMapper(Jsons.createObjectMapper(JsonInclude.Include.NON_NULL));
        messageConverter.setSupportedMediaTypes(Collects.concat(
            messageConverter.getSupportedMediaTypes(),
            MediaType.TEXT_PLAIN,
            MediaType.TEXT_HTML
        ));
        return messageConverter;
    }

    public static RestTemplate buildRestTemplate(int connectTimeout, int readTimeout) {
        //SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        requestFactory.setHttpContextFactory(new HttpContextFactory());
        return new RestTemplate(requestFactory);
    }

    public static MultiValueMap<String, String> toMultiValueMap(Map<String, Object> params) {
        if (MapUtils.isEmpty(params)) {
            return null;
        }
        Map<String, List<String>> map = params.entrySet()
            .stream()
            .filter(e -> StringUtils.isNotBlank(e.getKey()) && ObjectUtils.isNotEmpty(e.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, e -> toListString(e.getValue())));
        return MapUtils.isEmpty(map) ? null : new LinkedMultiValueMap<>(map);
    }

    public static class HttpContextHolder {
        private static final ThreadLocal<RequestConfig> THREAD_LOCAL = new NamedThreadLocal<>("request-config");

        public static void bind(RequestConfig requestConfig) {
            THREAD_LOCAL.set(requestConfig);
        }

        private static RequestConfig get() {
            return THREAD_LOCAL.get();
        }

        public static void unbind() {
            THREAD_LOCAL.remove();
        }
    }

    private static List<String> toListString(Object value) {
        if (value == null) {
            return null;
        }

        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            if (length == 0) {
                return Collections.emptyList();
            }
            List<String> list = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                list.add(toString(Array.get(value, i)));
            }
            return list;
        }

        if (value instanceof Collection) {
            Collection<?> coll = (Collection<?>) value;
            if (coll.isEmpty()) {
                return null;
            }
            return coll.stream().map(RestTemplateUtils::toString).collect(Collectors.toList());
        }

        return Collections.singletonList(toString(value));
    }

    private static String toString(Object value) {
        return value == null ? null : value.toString();
    }

    private static class HttpContextFactory implements BiFunction<HttpMethod, URI, HttpContext> {
        @Override
        public HttpContext apply(HttpMethod httpMethod, URI uri) {
            RequestConfig requestConfig = HttpContextHolder.get();
            if (requestConfig == null) {
                return null;
            }
            HttpContext context = HttpClientContext.create();
            context.setAttribute(HttpClientContext.REQUEST_CONFIG, requestConfig);
            return context;
        }
    }

}
