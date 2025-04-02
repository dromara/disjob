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
import cn.ponfee.disjob.common.util.Strings;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.registry.Discovery;
import cn.ponfee.disjob.registry.ServerRole;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Discovery server rest template(Method pattern)
 *
 * @param <D> the discovery type
 * @author Ponfee
 */
final class DiscoveryServerRestTemplate<D extends Server> {
    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryServerRestTemplate.class);

    private final Discovery<D> discoverServer;
    private final RestTemplate restTemplate;
    private final int retryMaxCount;
    private final long retryBackoffPeriod;

    DiscoveryServerRestTemplate(Discovery<D> discoverServer, RestTemplate restTemplate, RetryProperties retry) {
        retry.check();
        this.discoverServer = Objects.requireNonNull(discoverServer);
        this.restTemplate = Objects.requireNonNull(restTemplate);
        this.retryMaxCount = retry.getMaxCount();
        this.retryBackoffPeriod = retry.getBackoffPeriod();
    }

    /**
     * 当returnType=Void.class时：
     * 1）如果响应的为非异常的http状态码，则返回结果都是null
     * 2）如果响应的为异常的http状态码，则会抛出HttpStatusCodeException
     *
     * @param method     the method
     * @param group      the group name
     * @param httpMethod the http method
     * @param path       the request mapping path
     * @param args       the arguments
     * @param <T>        return type
     * @return invoked remote http response
     * @throws Exception if occur exception
     */
    <T> T execute(Method method, String group, HttpMethod httpMethod, String path, Object[] args) throws Exception {
        List<D> servers = discoverServer.getDiscoveredServers(group);
        ServerRole discoveryServerRole = discoverServer.discoveryRole();
        if (CollectionUtils.isEmpty(servers)) {
            String errMsg = (group == null ? " " : " '" + group + "' ");
            throw new IllegalStateException("Not found available" + errMsg + discoveryServerRole);
        }

        String serverContextPath;
        Map<String, String> authenticationHeaders = null;
        if (discoveryServerRole.isWorker()) {
            // Supervisor 远程调用 Worker
            serverContextPath = Supervisor.local().getWorkerContextPath(group);
        } else {
            // Worker 远程调用 Supervisor
            serverContextPath = Worker.local().getSupervisorContextPath();
            authenticationHeaders = Worker.local().createWorkerAuthenticationHeaders();
        }

        Type returnType = method.getGenericReturnType();
        String urlPath = Strings.concatPath(serverContextPath, path);
        Throwable ex = null;
        int serverNumber = servers.size();
        int start = ThreadLocalRandom.current().nextInt(serverNumber);
        for (int i = 0, n = Math.min(serverNumber, retryMaxCount); i <= n; i++) {
            Server server = servers.get((start + i) % serverNumber);
            String url = server.buildHttpUrlPrefix() + urlPath;
            try {
                return RestTemplateUtils.invoke(restTemplate, url, httpMethod, returnType, authenticationHeaders, args);
            } catch (Throwable e) {
                ex = e;
                LOG.error("Invoke server rpc failed [{}]: {}, {}, {}", i, url, Jsons.toJson(args), e.getMessage());
                if (DestinationServerRestTemplate.isNotRetry(e)) {
                    break;
                }
                if (i < n) {
                    Thread.sleep((i + 1) * retryBackoffPeriod);
                }
            }
        }

        String msg = (ex == null) ? null : ex.getMessage();
        if (StringUtils.isBlank(msg)) {
            msg = "Invoke server rpc error: " + urlPath;
        }
        throw new RpcInvokeException(msg, ex);
    }

}
