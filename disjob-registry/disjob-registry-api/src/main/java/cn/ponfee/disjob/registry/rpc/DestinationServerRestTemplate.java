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
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.Worker;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

/**
 * Destination server rest template(Method pattern)
 *
 * @author Ponfee
 */
final class DestinationServerRestTemplate {
    private static final Logger LOG = LoggerFactory.getLogger(DestinationServerRestTemplate.class);

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
     * @param arguments         the arguments
     * @param <T>               return type
     * @return invoked remote http response
     * @throws Exception if occur exception
     */
    <T> T invoke(Server destinationServer, String path, HttpMethod httpMethod, Type returnType, Object... arguments) throws Exception {
        Map<String, String> authenticationHeaders = null;
        Worker.Current currentWorker = Worker.current();
        if (destinationServer instanceof Supervisor && currentWorker != null) {
            // 这里可能存在Supervisor-A同时也为Worker-A角色，当Supervisor-A远程调用另一个Supervisor-B，
            // 此时会用Worker-A的身份认证信息去调用Supervisor-B，接收方Supervisor-B也会认为是Worker-A调用过来的，与实际情况不大相符
            authenticationHeaders = currentWorker.createWorkerAuthenticationHeaders();
        }

        String url = destinationServer.buildHttpUrlPrefix() + path;
        Throwable ex = null;
        for (int i = 0; i <= retryMaxCount; i++) {
            try {
                return RestTemplateUtils.invoke(restTemplate, url, httpMethod, returnType, authenticationHeaders, arguments);
            } catch (Throwable e) {
                ex = e;
                LOG.error("Invoke server rpc failed [{}]: {}, {}, {}", i, url, Jsons.toJson(arguments), e.getMessage());
                if (DiscoveryServerRestTemplate.isNotRetry(e)) {
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

}
