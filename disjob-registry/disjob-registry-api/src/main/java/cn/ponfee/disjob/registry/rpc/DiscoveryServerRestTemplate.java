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

    private final Discovery<D> discoveryServer;
    private final RestTemplate restTemplate;
    private final int retryMaxCount;
    private final long retryBackoffPeriod;

    DiscoveryServerRestTemplate(Discovery<D> discoveryServer, RestTemplate restTemplate, RetryProperties retry) {
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
     * @param args       the arguments
     * @param <T>        return type
     * @return invoked remote http response
     * @throws Exception if occur exception
     */
    <T> T execute(String group, String path, HttpMethod httpMethod, Type returnType, Object... args) throws Exception {
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
            serverContextPath = Supervisor.current().getWorkerContextPath(group);
        }
        int start = ThreadLocalRandom.current().nextInt(serverNumber);

        Throwable ex = null;
        for (int i = 0, n = Math.min(serverNumber, retryMaxCount); i <= n; i++) {
            Server server = servers.get((start + i) % serverNumber);
            String url = server.buildHttpUrlPrefix() + Strings.concatPath(serverContextPath, path);
            try {
                return RestTemplateUtils.invoke(restTemplate, url, httpMethod, returnType, authenticationHeaders, args);
            } catch (Throwable e) {
                ex = e;
                LOG.error("Invoke server rpc failed [{}]: {}, {}, {}", i, url, Jsons.toJson(args), e.getMessage());
                if (DestinationServerRestTemplate.isNotRetry(e)) {
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

}
