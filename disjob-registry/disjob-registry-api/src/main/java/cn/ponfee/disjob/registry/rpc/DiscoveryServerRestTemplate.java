/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry.rpc;

import cn.ponfee.disjob.common.spring.RestTemplateUtils;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.common.util.Strings;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.registry.Discovery;
import cn.ponfee.disjob.registry.ServerRole;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Discovery server rest template(Method pattern)
 *
 * @param <D> the discovery type
 * @author Ponfee
 */
final class DiscoveryServerRestTemplate<D extends Server> {
    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryServerRestTemplate.class);

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

    private final Discovery<D> discoveryServer;
    private final RestTemplate restTemplate;
    private final int retryMaxCount;
    private final long retryBackoffPeriod;

    DiscoveryServerRestTemplate(Discovery<D> discoveryServer,
                                RestTemplate restTemplate,
                                RetryProperties retry) {
        retry.check();
        this.discoveryServer = Objects.requireNonNull(discoveryServer);
        this.restTemplate = Objects.requireNonNull(restTemplate);
        this.retryMaxCount = retry.getMaxCount();
        this.retryBackoffPeriod = retry.getBackoffPeriod();
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
    <T> T execute(String group, String path, HttpMethod httpMethod, Type returnType, Object... arguments) throws Exception {
        List<D> servers = discoveryServer.getDiscoveredServers(group);
        ServerRole discoveryServerRole = discoveryServer.discoveryRole();
        if (CollectionUtils.isEmpty(servers)) {
            String errMsg = (group == null ? " " : " '" + group + "' ");
            throw new IllegalStateException("Not found available" + errMsg + discoveryServerRole);
        }

        int serverNumber = servers.size();
        Map<String, String> authenticationHeaders = null;
        String serverContextPath;
        if (discoveryServerRole == ServerRole.SUPERVISOR) {
            // Worker 远程调用 Supervisor
            serverContextPath = Worker.current().getSupervisorContextPath();
            authenticationHeaders = Worker.current().createWorkerAuthenticationHeaders();
        } else {
            // Supervisor 远程调用 Worker
            serverContextPath = Supervisor.current().getWorkerContextPath(((Worker) servers.get(0)).getGroup());
        }
        int start = ThreadLocalRandom.current().nextInt(serverNumber);

        Throwable ex = null;
        for (int i = 0, n = Math.min(serverNumber, retryMaxCount); i <= n; i++) {
            Server server = servers.get((start + i) % serverNumber);
            String url = server.buildHttpUrlPrefix() + Strings.concatUrlPath(serverContextPath, path);
            try {
                return RestTemplateUtils.invoke(restTemplate, url, httpMethod, returnType, authenticationHeaders, arguments);
            } catch (Throwable e) {
                ex = e;
                LOG.error("Invoke server rpc failed: {}, {}, {}", url, Jsons.toJson(arguments), e.getMessage());
                if (isNotRetry(e)) {
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
        throw new RpcInvokeException(msg, ex);
    }

    // ----------------------------------------------------------------------------------------static methods

    static boolean isNotRetry(Throwable e) {
        if (e == null) {
            return true;
        }
        if (isTimeoutException(e) || isTimeoutException(e.getCause())) {
            // org.springframework.web.client.ResourceAccessException#getCause may be timeout
            return false;
        }
        if (!(e instanceof HttpStatusCodeException)) {
            return true;
        }
        return !RETRIABLE_HTTP_STATUS.contains(((HttpStatusCodeException) e).getStatusCode());
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

}
