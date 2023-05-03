/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.spring;

import cn.ponfee.disjob.common.util.Collects;
import cn.ponfee.disjob.common.util.Jsons;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.MapUtils;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.lang.reflect.Array;
import java.net.URI;
import java.nio.charset.Charset;
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
    public static final Set<HttpMethod> QUERY_PARAM_METHODS = ImmutableSet.of(GET, DELETE, HEAD, OPTIONS);

    public static MappingJackson2HttpMessageConverter buildJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
        messageConverter.setObjectMapper(Jsons.createObjectMapper(JsonInclude.Include.NON_NULL));
        extensionSupportedMediaTypes(messageConverter);
        return messageConverter;
    }

    public static void extensionSupportedMediaTypes(MappingJackson2HttpMessageConverter converter) {
        List<MediaType> supportedMediaTypes = Collects.concat(
            converter.getSupportedMediaTypes(),
            MediaType.TEXT_PLAIN,
            MediaType.TEXT_HTML,
            MediaType.MULTIPART_FORM_DATA
        );
        converter.setSupportedMediaTypes(supportedMediaTypes);
    }

    public static RestTemplate buildRestTemplate(int connectTimeout, int readTimeout, Charset charset,
                                                 MappingJackson2HttpMessageConverter httpMessageConverter) {
        SSLContext sslContext;
        try {
            sslContext = SSLContexts.custom().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build();

            //sslContext = SSLContext.getInstance("TLS");
            //sslContext.init(null, new TrustManager[]{new SkipX509TrustManager()}, new SecureRandom());
        } catch (Exception e) {
            throw new SecurityException(e);
        }
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).build();

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

    // -----------------------------------------------------------------------static class

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
