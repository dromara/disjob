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

package cn.ponfee.disjob.registry.rpc;

import cn.ponfee.disjob.common.spring.RestTemplateUtils;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.Worker;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Destination server rest template(Method pattern)
 *
 * @author Ponfee
 */
final class DestinationServerRestTemplate {
    private static final Logger LOG = LoggerFactory.getLogger(DestinationServerRestTemplate.class);

    private static final Set<HttpStatus> RETRIABLE_HTTP_STATUS = ImmutableSet.of(
        // 4xx
        HttpStatus.REQUEST_TIMEOUT,
        //HttpStatus.CONFLICT,
        //HttpStatus.LOCKED,
        //HttpStatus.FAILED_DEPENDENCY,
        HttpStatus.TOO_EARLY,
        //HttpStatus.PRECONDITION_REQUIRED,
        HttpStatus.TOO_MANY_REQUESTS,

        // 5xx
        // 500：Supervisor内部组件超时(如数据库超时)等场景
        //HttpStatus.INTERNAL_SERVER_ERROR,
        HttpStatus.BAD_GATEWAY,
        HttpStatus.SERVICE_UNAVAILABLE,
        HttpStatus.GATEWAY_TIMEOUT,
        HttpStatus.BANDWIDTH_LIMIT_EXCEEDED
    );

    private final RestTemplate restTemplate;
    private final int retryMaxCount;
    private final long retryBackoffPeriod;

    DestinationServerRestTemplate(RestTemplate restTemplate, RetryProperties retry) {
        retry.check();
        this.restTemplate = Objects.requireNonNull(restTemplate);
        this.retryMaxCount = retry.getMaxCount();
        this.retryBackoffPeriod = retry.getBackoffPeriod();
    }

    /**
     * Invoke remote server
     *
     * @param destinationServer the destination server
     * @param httpMethod        the http method
     * @param returnType        the return type
     * @param args              the arguments
     * @param <T>               return type
     * @return invoked remote http response
     * @throws Exception if occur exception
     */
    <T> T invoke(Server destinationServer, String path, HttpMethod httpMethod, Type returnType, Object... args) throws Exception {
        Map<String, String> authenticationHeaders = null;
        if (destinationServer instanceof Supervisor && Worker.local() != null) {
            // 这里可能存在Supervisor-A同时也为Worker-A角色，当Supervisor-A远程调用另一个Supervisor-B，
            // 此时会用Worker-A的身份认证信息去调用Supervisor-B，接收方Supervisor-B也会认为是Worker-A调用过来的，与实际情况不大相符
            authenticationHeaders = Worker.local().createWorkerAuthenticationHeaders();
        }

        String url = destinationServer.buildHttpUrlPrefix() + path;
        Throwable ex = null;
        for (int i = 0; i <= retryMaxCount; i++) {
            try {
                return RestTemplateUtils.invoke(restTemplate, url, httpMethod, returnType, authenticationHeaders, args);
            } catch (Throwable e) {
                ex = e;
                LOG.error("Invoke server rpc failed [{}]: {}, {}, {}", i, url, Jsons.toJson(args), e.getMessage());
                if (isNotRetry(e)) {
                    break;
                }
                if (i < retryMaxCount) {
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
        // org.springframework.web.client.ResourceAccessException#getCause may be timeout
        Set<Throwable> set = new HashSet<>();
        for (Throwable t = e; t != null && set.add(t); ) {
            if (isTimeoutException(t)) {
                return false;
            }
            t = t.getCause();
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
            || (e instanceof java.net.SocketException)
            || (e instanceof java.rmi.ConnectException)
            || (e instanceof java.rmi.ConnectIOException)
            || (e instanceof org.apache.http.conn.ConnectTimeoutException);
    }

}
