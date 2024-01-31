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
import cn.ponfee.disjob.core.base.*;
import cn.ponfee.disjob.registry.RPCInvokeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Server rest template(Method pattern)
 *
 * @author Ponfee
 */
final class ServerRestTemplate {

    private static final Logger LOG = LoggerFactory.getLogger(ServerRestTemplate.class);

    private final RestTemplate restTemplate;
    private final int retryMaxCount;
    private final long retryBackoffPeriod;

    ServerRestTemplate(HttpProperties http, RetryProperties retry, ObjectMapper objectMapper) {
        retry.check();
        this.restTemplate = DiscoveryRestTemplate.createRestTemplate(http, objectMapper);
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
            authenticationHeaders = currentWorker.createWorkerAuthenticationHeaders();
        }

        String url = String.format("http://%s:%d/%s", destinationServer.getHost(), destinationServer.getPort(), path);
        Throwable ex = null;
        for (int i = 0; i <= retryMaxCount; i++) {
            try {
                return RestTemplateUtils.invokeRpc(restTemplate, url, httpMethod, returnType, authenticationHeaders, arguments);
            } catch (Throwable e) {
                ex = e;
                LOG.error("Invoke server rpc failed: {}, {}, {}", url, Jsons.toJson(arguments), e.getMessage());
                if (!DiscoveryRestTemplate.isRequireRetry(e)) {
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
        throw new RPCInvokeException(msg, ex);
    }

}
