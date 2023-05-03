/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry;

import cn.ponfee.disjob.common.model.Result;
import cn.ponfee.disjob.common.spring.RestTemplateUtils;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.base.Server;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Retry rest template(Method pattern)
 *
 * @param <D> the discovery type
 * @author Ponfee
 */
public class DiscoveryRestTemplate<D extends Server> {

    private final static Logger LOG = LoggerFactory.getLogger(DiscoveryRestTemplate.class);

    public static final Type RESULT_STRING = new ParameterizedTypeReference<Result<String>>() {}.getType();
    public static final Type RESULT_BOOLEAN = new ParameterizedTypeReference<Result<Boolean>>() {}.getType();
    public static final Type RESULT_VOID = new ParameterizedTypeReference<Result<Void>>() {}.getType();
    public static final Object[] EMPTY = new Object[0];

    private final RestTemplate restTemplate;
    private final Discovery<D> discoveryServer;
    private final int maxRetryTimes;

    private DiscoveryRestTemplate(int connectTimeout,
                                  int readTimeout,
                                  ObjectMapper objectMapper,
                                  Discovery<D> discoveryServer,
                                  int maxRetryTimes) {
        MappingJackson2HttpMessageConverter httpMessageConverter = new MappingJackson2HttpMessageConverter();
        httpMessageConverter.setObjectMapper(objectMapper);
        RestTemplateUtils.extensionSupportedMediaTypes(httpMessageConverter);

        this.restTemplate = RestTemplateUtils.buildRestTemplate(
            connectTimeout, readTimeout, StandardCharsets.UTF_8, httpMessageConverter
        );
        this.discoveryServer = discoveryServer;
        this.maxRetryTimes = maxRetryTimes;
    }

    /**
     * 当returnType=Void.class时：
     * 1）如果响应的为非异常的http状态码，则返回结果都是null
     * 2）如果响应的为异常的http状态码，则会抛出HttpStatusCodeException
     *
     * @param group      the group name
     * @param path       the url path
     * @param httpMethod the http method
     * @param returnType the return type
     * @param arguments  the arguments
     * @param <T>        return type
     * @return invoked remote http response
     * @throws Exception if occur exception
     */
    public <T> T execute(String group, String path, HttpMethod httpMethod, Type returnType, Object... arguments) throws Exception {
        List<D> servers = discoveryServer.getDiscoveredServers(group);
        if (CollectionUtils.isEmpty(servers)) {
            String errMsg = (group == null ? " " : " '" + group + "' ");
            throw new IllegalStateException("Not found available" + errMsg + discoveryServer.discoveryRole());
        }

        int serverNumber = servers.size();
        int start = ThreadLocalRandom.current().nextInt(serverNumber);
        // minimum retry two times
        Exception ex = null;
        for (int i = 0, n = Math.min(serverNumber, maxRetryTimes); i <= n; i++) {
            Server server = servers.get((start + i) % serverNumber);
            String url = String.format("http://%s:%d/%s", server.getHost(), server.getPort(), path);
            try {
                URI uri;
                HttpEntity<?> httpEntity;
                if (RestTemplateUtils.QUERY_PARAM_METHODS.contains(httpMethod)) {
                    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
                    if (ArrayUtils.isNotEmpty(arguments)) {
                        builder.queryParams(buildQueryParams(arguments));
                    }
                    uri = builder.build().encode().toUri();
                    httpEntity = null;
                } else {
                    uri = restTemplate.getUriTemplateHandler().expand(url, EMPTY);
                    httpEntity = ArrayUtils.isEmpty(arguments) ? null : new HttpEntity<>(arguments);
                }

                RequestCallback requestCallback = restTemplate.httpEntityCallback(httpEntity, returnType);
                ResponseExtractor<ResponseEntity<T>> responseExtractor = restTemplate.responseEntityExtractor(returnType);
                return restTemplate.execute(uri, httpMethod, requestCallback, responseExtractor).getBody();
            } catch (Exception e) {
                ex = e;
                if (e instanceof ResourceAccessException || is5xxServerError(e)) {
                    LOG.error("Invoke discovered server rpc fail: {} | {} | {}", url, Jsons.toJson(arguments), e.getMessage());
                } else {
                    LOG.error("Invoke discovered server rpc error: {} | {} | {}", url, Jsons.toJson(arguments), e.getMessage());
                }

                if (i < n) {
                    // round-robin retry, 100L * IntMath.pow(i + 1, 2)
                    Thread.sleep(serverNumber == 1 ? 2000 : 1000L * (i + 1));
                }
            }
        }

        throw new IllegalStateException("Invoke http retried failed: " + path + ", " + Jsons.toJson(arguments), ex);
    }

    public Discovery<D> getDiscoveryServer() {
        return discoveryServer;
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    private static MultiValueMap<String, String> buildQueryParams(Object[] arguments) {
        if (ArrayUtils.isEmpty(arguments)) {
            return null;
        }
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(arguments.length << 1);
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] != null) {
                params.add("args[" + i + "]", Jsons.toJson(arguments[i]));
            }
        }
        return params;
    }

    private static boolean is5xxServerError(Exception e) {
        if (!(e instanceof HttpStatusCodeException)) {
            return false;
        }
        return ((HttpStatusCodeException) e).getStatusCode().is5xxServerError();
    }

    // ----------------------------------------------------------------------------------------builder

    public static <S extends Server> Builder<S> builder() {
        return new Builder<>();
    }

    public static class Builder<S extends Server> {
        private int connectTimeout;
        private int readTimeout;
        private ObjectMapper objectMapper;
        private Discovery<S> discoveryServer;
        private int maxRetryTimes;

        private Builder() {
        }

        public Builder<S> connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder<S> readTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder<S> objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public Builder<S> discoveryServer(Discovery<S> discoveryServer) {
            this.discoveryServer = discoveryServer;
            return this;
        }

        public Builder<S> maxRetryTimes(int maxRetryTimes) {
            this.maxRetryTimes = maxRetryTimes;
            return this;
        }

        public DiscoveryRestTemplate<S> build() {
            return new DiscoveryRestTemplate<>(connectTimeout, readTimeout, objectMapper, discoveryServer, maxRetryTimes);
        }
    }

}
