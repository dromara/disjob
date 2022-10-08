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
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.web.client.*;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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

    public <T> T execute(String path, Type returnType, Object... arguments) throws Exception {
        return doExecute(null, path, returnType, arguments);
    }

    public <T> T execute(String group, String path, Type returnType, Object... arguments) throws Exception {
        return doExecute(group, path, returnType, arguments);
    }

    /**
     * 如果returnType=Void.class时：
     * 1）如果响应的为非异常的http状态码，则返回结果都是null
     * 2）如果响应的为异常的http状态码，则会抛出HttpStatusCodeException
     *
     * @param group      the group name
     * @param path       the uri path
     * @param returnType the method return type
     * @param arguments  the method arguments
     * @param <T>        then return type
     * @return result
     * @throws Exception if occur exception
     */
    private <T> T doExecute(String group, String path, Type returnType, Object... arguments) throws Exception {
        List<D> servers = discoveryServer.getServers(group);
        if (CollectionUtils.isEmpty(servers)) {
            throw new IllegalStateException("Not found available " + discoveryServer.discoveryRole().name() + " servers");
        }

        int serverNumber = servers.size();
        int start = ThreadLocalRandom.current().nextInt(serverNumber);
        for (int i = 0, n = Math.min(serverNumber, maxRetryTimes) + 1; i < n; i++) {
            Server server = servers.get((start + i) % serverNumber);
            String url = String.format("http://%s:%d/%s", server.getHost(), server.getPort(), path);
            try {
                RequestCallback requestCallback = restTemplate.httpEntityCallback(new HttpEntity<>(arguments), returnType);
                ResponseExtractor<ResponseEntity<T>> responseExtractor = restTemplate.responseEntityExtractor(returnType);
                ResponseEntity<T> resp = restTemplate.execute(url, HttpMethod.POST, requestCallback, responseExtractor);
                Assert.notNull(resp, "No response");
                Object body = resp.getBody();
                if (body instanceof Result) {
                    Result<T> result = (Result<T>) body;
                    if (result.isSuccess()) {
                        return result.getData();
                    } else {
                        throw new IllegalStateException("Invoked http failed, url: " + url + ", req: " + Jsons.toJson(arguments) + ", res: " + result);
                    }
                } else {
                    return (T) body;
                }
            } catch (ResourceAccessException | RestClientResponseException e) {
                // round-robin retry
                LOG.error("Invoked http error, url: " + url + ", req: " + Jsons.toJson(arguments), e);
                Thread.sleep(300L * IntMath.pow(i + 1, 2));
            }
        }

        throw new IllegalStateException("Invoke http retried failed: " + path + ", " + Jsons.toJson(arguments));
    }

    public Discovery<D> getDiscoveryServer() {
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
