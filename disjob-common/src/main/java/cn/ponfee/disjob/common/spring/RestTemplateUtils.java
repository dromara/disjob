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

import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingSupplier;
import cn.ponfee.disjob.common.model.Result;
import cn.ponfee.disjob.common.util.Jsons;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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

    /**
     * <pre>
     * HttpComponentsClientHttpRequestFactory#createHttpUriRequest中，继承HttpRequestBase的method都不支持传body
     *
     * 可以自定义一个HttpComponentsClientHttpRequestFactory子类，
     * 重写createHttpUriRequest方法并返回HttpEntityEnclosingRequestBase子类来支持传body
     *
     * 以下是不支持传body的http method（注：HttpMethod.DELETE可以传body）
     * </pre>
     */
    public static final Set<HttpMethod> QUERY_PARAM_METHODS = ImmutableSet.of(GET, HEAD, OPTIONS, TRACE);

    /**
     * Thread local for request config
     */
    private static final ThreadLocal<RequestConfig> REQUEST_CONFIG_THREAD_LOCAL = new NamedThreadLocal<>("request-config");

    public static final Type RESULT_STRING = new ParameterizedTypeReference<Result<String>>() {}.getType();
    public static final Type RESULT_BOOLEAN = new ParameterizedTypeReference<Result<Boolean>>() {}.getType();
    public static final Type RESULT_VOID = new ParameterizedTypeReference<Result<Void>>() {}.getType();

    public static RestTemplate create(int connectTimeout, int readTimeout, ObjectMapper objectMapper) {
        return create(connectTimeout, readTimeout, objectMapper, StandardCharsets.UTF_8);
    }

    public static RestTemplate create(int connectTimeout, int readTimeout, ObjectMapper objectMapper, Charset charset) {
        return create(connectTimeout, readTimeout, createMappingJackson2HttpMessageConverter(objectMapper), charset);
    }

    public static RestTemplate create(int connectTimeout, int readTimeout, MappingJackson2HttpMessageConverter messageConverter, Charset charset) {
        SSLContext sslContext = ThrowingSupplier.doChecked(() -> SSLContexts.custom().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build());
        CloseableHttpClient httpClient = HttpClients.custom()
            .setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
            .build();

        //SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        requestFactory.setHttpContextFactory(new HttpContextFactory());

        RestTemplate restTemplate = new RestTemplate(requestFactory);
        restTemplate.setMessageConverters(Arrays.asList(
            new ByteArrayHttpMessageConverter(),
            new StringHttpMessageConverter(charset),
            new ResourceHttpMessageConverter(),
            new SourceHttpMessageConverter<>(),
            new FormHttpMessageConverter(),
            messageConverter
        ));
        return restTemplate;
    }

    public static MappingJackson2HttpMessageConverter createMappingJackson2HttpMessageConverter(@Nullable ObjectMapper objectMapper) {
        if (objectMapper == null) {
            objectMapper = Jsons.createObjectMapper(JsonInclude.Include.NON_NULL);
        }
        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
        messageConverter.setObjectMapper(objectMapper);
        extendSupportedMediaTypes(messageConverter);
        return messageConverter;
    }

    public static void extendSupportedMediaTypes(MappingJackson2HttpMessageConverter converter) {
        List<MediaType> supportedMediaTypes = Collects.concat(
            converter.getSupportedMediaTypes(),
            MediaType.TEXT_PLAIN,
            MediaType.TEXT_HTML,
            MediaType.MULTIPART_FORM_DATA,
            MediaType.ALL
        );
        converter.setSupportedMediaTypes(supportedMediaTypes);
    }

    public static MultiValueMap<String, String> convertToMultiValueMap(Map<String, Object> params) {
        if (MapUtils.isEmpty(params)) {
            return null;
        }
        Map<String, List<String>> map = params.entrySet()
            .stream()
            .filter(e -> StringUtils.isNotBlank(e.getKey()) && ObjectUtils.isNotEmpty(e.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, e -> toListString(e.getValue())));
        return MapUtils.isEmpty(map) ? null : new LinkedMultiValueMap<>(map);
    }

    /**
     * Rpc invoke based http
     *
     * @param restTemplate the restTemplate
     * @param url          the url
     * @param httpMethod   the httpMethod
     * @param returnType   the returnType
     * @param headersMap   the headersMap
     * @param arguments    the arguments
     * @param <T>          result type
     * @return result object
     * @see RpcControllerConfigurer
     */
    public static <T> T invoke(RestTemplate restTemplate, String url, HttpMethod httpMethod,
                               Type returnType, Map<String, String> headersMap, Object... arguments) {
        HttpHeaders headers = new HttpHeaders();
        if (MapUtils.isNotEmpty(headersMap)) {
            headersMap.forEach(headers::set);
        }

        URI uri;
        if (QUERY_PARAM_METHODS.contains(httpMethod)) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
            if (ArrayUtils.isNotEmpty(arguments)) {
                builder.queryParams(RpcControllerUtils.buildQueryParameters(arguments));
            }
            uri = builder.build().encode().toUri();
            arguments = null;
        } else {
            uri = restTemplate.getUriTemplateHandler().expand(url, Collects.EMPTY_OBJECT_ARRAY);
            if (ArrayUtils.isNotEmpty(arguments)) {
                headers.setContentType(MediaType.APPLICATION_JSON);
            }
        }

        RequestCallback requestCallback = restTemplate.httpEntityCallback(new HttpEntity<>(arguments, headers), returnType);
        ResponseExtractor<ResponseEntity<T>> responseExtractor = restTemplate.responseEntityExtractor(returnType);
        ResponseEntity<T> responseEntity = restTemplate.execute(uri, httpMethod, requestCallback, responseExtractor);
        return Objects.requireNonNull(responseEntity).getBody();
    }

    public static <T> T invoke(RequestConfig requestConfig, RestTemplate restTemplate, URI uri,
                               HttpMethod method, RequestCallback requestCallback, ResponseExtractor<T> responseExtractor) {
        if (requestConfig == null) {
            return restTemplate.execute(uri, method, requestCallback, responseExtractor);
        }

        REQUEST_CONFIG_THREAD_LOCAL.set(requestConfig);
        try {
            return restTemplate.execute(uri, method, requestCallback, responseExtractor);
        } finally {
            REQUEST_CONFIG_THREAD_LOCAL.remove();
        }
    }

    // -----------------------------------------------------------------------private methods or static class

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
            return coll.isEmpty() ? null : coll.stream().map(RestTemplateUtils::toString).collect(Collectors.toList());
        }

        return Collections.singletonList(toString(value));
    }

    private static String toString(Object value) {
        return value == null ? null : value.toString();
    }

    private static class HttpContextFactory implements BiFunction<HttpMethod, URI, HttpContext> {
        @Override
        public HttpContext apply(HttpMethod httpMethod, URI uri) {
            RequestConfig requestConfig = REQUEST_CONFIG_THREAD_LOCAL.get();
            if (requestConfig == null) {
                return null;
            }
            HttpClientContext context = HttpClientContext.create();
            context.setRequestConfig(requestConfig);
            return context;
        }
    }

}
