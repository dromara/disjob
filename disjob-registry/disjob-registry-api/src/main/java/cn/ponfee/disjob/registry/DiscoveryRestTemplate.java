/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry;

import cn.ponfee.disjob.common.spring.RestTemplateUtils;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.core.base.Worker;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Retry rest template(Method pattern)
 *
 * @param <D> the discovery type
 * @author Ponfee
 */
public final class DiscoveryRestTemplate<D extends Server> {

    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryRestTemplate.class);

    private static final Set<HttpStatus> RETRIABLE_HTTP_STATUS = ImmutableSet.of(
        // 4xx
        HttpStatus.REQUEST_TIMEOUT,
        HttpStatus.CONFLICT,
        HttpStatus.LOCKED,
        HttpStatus.FAILED_DEPENDENCY,
        HttpStatus.TOO_EARLY,
        HttpStatus.PRECONDITION_REQUIRED,
        HttpStatus.TOO_MANY_REQUESTS,

        // 5xx
        // 500：Supervisor内部组件超时(如数据库超时)等场景
        HttpStatus.INTERNAL_SERVER_ERROR,
        HttpStatus.BAD_GATEWAY,
        HttpStatus.SERVICE_UNAVAILABLE,
        HttpStatus.GATEWAY_TIMEOUT,
        HttpStatus.BANDWIDTH_LIMIT_EXCEEDED
    );

    private final RestTemplate restTemplate;
    private final Discovery<D> discoveryServer;
    private final int retryMaxCount;
    private final long retryBackoffPeriod;

    private DiscoveryRestTemplate(int httpConnectTimeout,
                                  int httpReadTimeout,
                                  int retryMaxCount,
                                  int retryBackoffPeriod,
                                  ObjectMapper objectMapper,
                                  Discovery<D> discoveryServer) {
        Assert.isTrue(httpConnectTimeout > 0, "Http connect timeout must be greater than 0.");
        Assert.isTrue(httpReadTimeout > 0, "Http read timeout must be greater than 0.");
        Assert.isTrue(retryMaxCount >= 0, "Retry max count cannot less than 0.");
        Assert.isTrue(retryBackoffPeriod > 0, "Retry backoff period must be greater than 0.");

        if (objectMapper == null) {
            objectMapper = Jsons.createObjectMapper(JsonInclude.Include.NON_NULL);
        }
        MappingJackson2HttpMessageConverter httpMessageConverter = new MappingJackson2HttpMessageConverter();
        httpMessageConverter.setObjectMapper(objectMapper);
        RestTemplateUtils.extensionSupportedMediaTypes(httpMessageConverter);

        this.restTemplate = RestTemplateUtils.buildRestTemplate(
            httpConnectTimeout, httpReadTimeout, StandardCharsets.UTF_8, httpMessageConverter
        );
        this.discoveryServer = discoveryServer;
        this.retryMaxCount = retryMaxCount;
        this.retryBackoffPeriod = retryBackoffPeriod;
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
        ServerRole discoveryServerRole = discoveryServer.discoveryRole();
        if (CollectionUtils.isEmpty(servers)) {
            String errMsg = (group == null ? " " : " '" + group + "' ");
            throw new IllegalStateException("Not found available" + errMsg + discoveryServerRole);
        }

        int serverNumber = servers.size();
        Map<String, String> authenticationHeaders = null;
        if (discoveryServerRole == ServerRole.SUPERVISOR) {
            authenticationHeaders = Worker.current().authenticationHeaders();
        }
        int start = ThreadLocalRandom.current().nextInt(serverNumber);

        // minimum retry two times
        Throwable ex = null;
        for (int i = 0, n = Math.min(serverNumber, retryMaxCount); i <= n; i++) {
            Server server = servers.get((start + i) % serverNumber);
            String url = String.format("http://%s:%d/%s", server.getHost(), server.getPort(), path);
            try {
                return RestTemplateUtils.invokeRpc(restTemplate, url, httpMethod, returnType, authenticationHeaders, arguments);
            } catch (Throwable e) {
                ex = e;
                LOG.error("Invoke server rpc failed: {}, {}, {}", url, Jsons.toJson(arguments), e.getMessage());
                if (!isRequireRetry(e)) {
                    break;
                }
                if (i < n) {
                    // round-robin retry, 100L * IntMath.pow(i + 1, 2)
                    Thread.sleep((i + 1) * retryBackoffPeriod);
                }
            }
        }

        String msg = (ex == null) ? null : ex.getMessage();
        if (StringUtils.isBlank(msg)) {
            msg = "Invoke server rpc error: " + path;
        }
        throw new RPCInvokeException(msg, ex);
    }

    public Discovery<D> getDiscoveryServer() {
        return discoveryServer;
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    // ----------------------------------------------------------------------------------------private methods

    private static boolean isRequireRetry(Throwable e) {
        if (e == null) {
            return false;
        }
        if (isTimeoutException(e) || isTimeoutException(e.getCause())) {
            // org.springframework.web.client.ResourceAccessException#getCause may be timeout
            return true;
        }
        if (!(e instanceof HttpStatusCodeException)) {
            return false;
        }
        return RETRIABLE_HTTP_STATUS.contains(((HttpStatusCodeException) e).getStatusCode());
    }

    private static boolean isTimeoutException(Throwable e) {
        if (e == null) {
            return false;
        }
        return (e instanceof java.net.SocketTimeoutException)
            || (e instanceof java.net.ConnectException)
            || (e instanceof org.apache.http.conn.ConnectTimeoutException)
            || (e instanceof java.rmi.ConnectException);
    }

    // ----------------------------------------------------------------------------------------builder

    public static <S extends Server> Builder<S> builder() {
        return new Builder<>();
    }

    public static class Builder<S extends Server> {
        private int httpConnectTimeout;
        private int httpReadTimeout;
        private int retryMaxCount;
        private int retryBackoffPeriod;
        private ObjectMapper objectMapper;
        private Discovery<S> discoveryServer;

        private Builder() {
        }

        public Builder<S> httpConnectTimeout(int httpConnectTimeout) {
            this.httpConnectTimeout = httpConnectTimeout;
            return this;
        }

        public Builder<S> httpReadTimeout(int httpReadTimeout) {
            this.httpReadTimeout = httpReadTimeout;
            return this;
        }

        public Builder<S> retryMaxCount(int retryMaxCount) {
            this.retryMaxCount = retryMaxCount;
            return this;
        }

        public Builder<S> retryBackoffPeriod(int retryBackoffPeriod) {
            this.retryBackoffPeriod = retryBackoffPeriod;
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

        public DiscoveryRestTemplate<S> build() {
            return new DiscoveryRestTemplate<>(
                httpConnectTimeout, httpReadTimeout, retryMaxCount, retryBackoffPeriod, objectMapper, discoveryServer
            );
        }
    }

}
