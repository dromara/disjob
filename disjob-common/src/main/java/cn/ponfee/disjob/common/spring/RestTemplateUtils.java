/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.spring;

import cn.ponfee.disjob.common.collect.Collects;
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

    public static final Type RESULT_STRING = new ParameterizedTypeReference<Result<String>>() {}.getType();
    public static final Type RESULT_BOOLEAN = new ParameterizedTypeReference<Result<Boolean>>() {}.getType();
    public static final Type RESULT_VOID = new ParameterizedTypeReference<Result<Void>>() {}.getType();
    public static final Object[] EMPTY = new Object[0];

    public static MappingJackson2HttpMessageConverter buildJackson2HttpMessageConverter(ObjectMapper objectMapper) {
        if (objectMapper == null) {
            objectMapper = Jsons.createObjectMapper(JsonInclude.Include.NON_NULL);
        }
        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
        messageConverter.setObjectMapper(objectMapper);
        extensionSupportedMediaTypes(messageConverter);
        return messageConverter;
    }

    public static void extensionSupportedMediaTypes(MappingJackson2HttpMessageConverter converter) {
        List<MediaType> supportedMediaTypes = Collects.concat(
            converter.getSupportedMediaTypes(),
            MediaType.TEXT_PLAIN,
            MediaType.TEXT_HTML,
            MediaType.MULTIPART_FORM_DATA,
            MediaType.ALL
        );
        converter.setSupportedMediaTypes(supportedMediaTypes);
    }

    public static RestTemplate buildRestTemplate(int connectTimeout, int readTimeout, ObjectMapper objectMapper) {
        return buildRestTemplate(connectTimeout, readTimeout, StandardCharsets.UTF_8, objectMapper);
    }

    public static RestTemplate buildRestTemplate(int connectTimeout,
                                                 int readTimeout,
                                                 Charset charset,
                                                 ObjectMapper objectMapper) {
        if (objectMapper == null) {
            objectMapper = Jsons.createObjectMapper(JsonInclude.Include.NON_NULL);
        }
        MappingJackson2HttpMessageConverter httpMessageConverter = new MappingJackson2HttpMessageConverter();
        httpMessageConverter.setObjectMapper(objectMapper);
        RestTemplateUtils.extensionSupportedMediaTypes(httpMessageConverter);
        return buildRestTemplate(connectTimeout, readTimeout, charset, httpMessageConverter);
    }

    public static RestTemplate buildRestTemplate(int connectTimeout,
                                                 int readTimeout,
                                                 Charset charset,
                                                 MappingJackson2HttpMessageConverter httpMessageConverter) {
        SSLContext sslContext;
        try {
            sslContext = SSLContexts.custom().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build();

            //sslContext = SSLContext.getInstance("TLS");
            //sslContext.init(null, new TrustManager[]{new SkipX509TrustManager()}, new SecureRandom());
        } catch (Exception e) {
            throw new SecurityException(e);
        }

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
            httpMessageConverter
        ));
        return restTemplate;
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
     * @see LocalizedMethodArguments
     * @see LocalizedMethodArgumentResolver
     */
    public static <T> T invoke(RestTemplate restTemplate, String url, HttpMethod httpMethod,
                               Type returnType, Map<String, String> headersMap, Object... arguments) {
        HttpHeaders headers = new HttpHeaders();
        if (MapUtils.isNotEmpty(headersMap)) {
            headersMap.forEach(headers::set);
        }

        URI uri;
        if (QUERY_PARAM_METHODS.contains(httpMethod)) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
            if (ArrayUtils.isNotEmpty(arguments)) {
                builder.queryParams(LocalizedMethodArgumentUtils.buildQueryParams(arguments));
            }
            uri = builder.build().encode().toUri();
            arguments = null;
        } else {
            uri = restTemplate.getUriTemplateHandler().expand(url, EMPTY);
            if (ArrayUtils.isNotEmpty(arguments)) {
                headers.setContentType(MediaType.APPLICATION_JSON);
            }
        }

        RequestCallback requestCallback = restTemplate.httpEntityCallback(new HttpEntity<>(arguments, headers), returnType);
        ResponseExtractor<ResponseEntity<T>> responseExtractor = restTemplate.responseEntityExtractor(returnType);
        ResponseEntity<T> responseEntity = restTemplate.execute(uri, httpMethod, requestCallback, responseExtractor);
        return Objects.requireNonNull(responseEntity).getBody();
    }

    // -----------------------------------------------------------------------public static class

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
            RequestConfig requestConfig = HttpContextHolder.get();
            if (requestConfig == null) {
                return null;
            }
            HttpContext context = HttpClientContext.create();
            context.setAttribute(HttpClientContext.REQUEST_CONFIG, requestConfig);
            return context;
        }
    }

    /*
    private static class SkipX509TrustManager implements X509TrustManager {
        private static final X509Certificate[] EMPTY = new X509Certificate[0];
        @Override
        public X509Certificate[] getAcceptedIssuers() { return EMPTY; }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) { }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) { }
    }
    */

}
