package cn.ponfee.scheduler.registry;

import cn.ponfee.scheduler.common.base.model.Result;
import cn.ponfee.scheduler.common.util.Collects;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.base.Server;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.math.IntMath;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Retry rest template(Method pattern)
 * 
 * @author Ponfee
 */
public class DiscoveryRestTemplate<S extends Server> {

    private final static Logger LOG = LoggerFactory.getLogger(DiscoveryRestTemplate.class);

    public static final ParameterizedTypeReference<Result<String>>   RESULT_STRING = new ParameterizedTypeReference<Result<String>>() {};
    public static final ParameterizedTypeReference<Result<Boolean>> RESULT_BOOLEAN = new ParameterizedTypeReference<Result<Boolean>>() {};
    public static final ParameterizedTypeReference<Result<Void>>       RESULT_VOID = new ParameterizedTypeReference<Result<Void>>() {};

    private final RestTemplate restTemplate;
    private final Discovery<S> discoveryServer;
    private final int maxRetryTimes;

    private DiscoveryRestTemplate(int connectTimeout,
                                  int readTimeout,
                                  ObjectMapper objectMapper,
                                  Discovery<S> discoveryServer,
                                  int maxRetryTimes) {
        MappingJackson2HttpMessageConverter httpMessageConverter = new MappingJackson2HttpMessageConverter();
        httpMessageConverter.setObjectMapper(objectMapper);
        httpMessageConverter.setSupportedMediaTypes(Collects.concat(httpMessageConverter.getSupportedMediaTypes(), MediaType.TEXT_PLAIN));

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
            httpMessageConverter
        ));

        this.restTemplate = restTemplate;
        this.discoveryServer = discoveryServer;
        this.maxRetryTimes = maxRetryTimes;
    }

    public <T> T execute(String path, Class<T> returnType, Object... arguments) throws Exception {
        return doExecute(null, path, returnType, arguments);
    }

    public <T> T execute(String group, String path, Class<T> returnType, Object... arguments) throws Exception {
        return doExecute(group, path, returnType, arguments);
    }

    public <T> T execute(String path, ParameterizedTypeReference<Result<T>> returnType, Object... arguments) throws Exception {
        return doExecute(null, path, returnType, arguments);
    }

    public <T> T execute(String group, String path, ParameterizedTypeReference<Result<T>> returnType, Object... arguments) throws Exception {
        return doExecute(group, path, returnType, arguments);
    }

    private <T> T doExecute(String group, String path, Object returnType, Object... arguments) throws Exception {
        List<S> servers = discoveryServer.getServers(group);
        if (CollectionUtils.isEmpty(servers)) {
            throw new IllegalStateException("Not found available " + discoveryServer.discoveryRole() + " servers");
        }

        int serverNumber = servers.size();
        int start = ThreadLocalRandom.current().nextInt(serverNumber);
        for (int i = 0, n = Math.min(serverNumber, maxRetryTimes) + 1; i < n; i++) {
            Server server = servers.get((start + i) % serverNumber);
            String url = String.format("http://%s:%d/%s", server.getHost(), server.getPort(), path);
            try {
                if (returnType instanceof ParameterizedTypeReference) {
                    Result<T> result = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(arguments), (ParameterizedTypeReference<Result<T>>) returnType).getBody();
                    if (result.isSuccess()) {
                        return result.getData();
                    } else {
                        throw new IllegalStateException("Invoked http failed, url: " + url + ", req: " + Jsons.toJson(arguments) + ", res: " + result);
                    }
                } else if (returnType instanceof Class<?>) {
                    // 如果returnType=Void.class时：
                    //   1）如果响应的为非异常的http状态码，则返回结果都是null
                    //   2）如果响应的为异常的http状态码，则会抛出HttpStatusCodeException
                    return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(arguments), (Class<T>) returnType).getBody();
                } else {
                    throw new IllegalArgumentException("Invalid return type: " + returnType);
                }
            } catch (ResourceAccessException | RestClientResponseException e) {
                // round-robin retry
                LOG.error("Invoked http error, url: " + url + ", req: " + Jsons.toJson(arguments), e);
                Thread.sleep(300 * IntMath.pow(i + 1, 2));
            }
        }

        throw new IllegalStateException("Invoke http retried failed: " + path + ", " + Jsons.toJson(arguments));
    }

    public Discovery<S> getDiscoveryServer() {
        return discoveryServer;
    }

    // ----------------------------------------------------------------------------------------builder

    public static <S extends Server> DiscoveryRestTemplateBuilder<S> builder() {
        return new DiscoveryRestTemplateBuilder<>();
    }

    public static class DiscoveryRestTemplateBuilder<S extends Server> {
        private int connectTimeout;
        private int readTimeout;
        private ObjectMapper objectMapper;
        private Discovery<S> discoveryServer;
        private int maxRetryTimes;

        public DiscoveryRestTemplateBuilder<S> connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public DiscoveryRestTemplateBuilder<S> readTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public DiscoveryRestTemplateBuilder<S> objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public DiscoveryRestTemplateBuilder<S> discoveryServer(Discovery<S> discoveryServer) {
            this.discoveryServer = discoveryServer;
            return this;
        }

        public DiscoveryRestTemplateBuilder<S> maxRetryTimes(int maxRetryTimes) {
            this.maxRetryTimes = maxRetryTimes;
            return this;
        }

        public DiscoveryRestTemplate<S> build() {
            return new DiscoveryRestTemplate<>(
                connectTimeout, readTimeout, objectMapper, discoveryServer, maxRetryTimes
            );
        }
    }

}
