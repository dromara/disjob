/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application;

import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.spring.RestTemplateUtils;
import cn.ponfee.disjob.core.base.HttpProperties;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.SupervisorRpcService;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.supervisor.application.response.SupervisorInfoResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Server info service
 *
 * @author Ponfee
 */
@Component
public class ServerInfoService extends SingletonClassConstraint {
    private static final Logger LOG = LoggerFactory.getLogger(ServerInfoService.class);
    private static final String IS_WORKER_URL_PATH = "http://%s:%d/" + SupervisorRpcService.PREFIX_PATH + "is_worker";

    private final RestTemplate restTemplate;
    private final SupervisorRegistry supervisorRegistry;
    private final SupervisorRpcService supervisorRpcService;

    public ServerInfoService(HttpProperties http,
                             ObjectMapper objectMapper,
                             SupervisorRegistry supervisorRegistry,
                             SupervisorRpcService supervisorRpcService) {
        MappingJackson2HttpMessageConverter httpMessageConverter = new MappingJackson2HttpMessageConverter();
        httpMessageConverter.setObjectMapper(objectMapper);
        RestTemplateUtils.extensionSupportedMediaTypes(httpMessageConverter);
        this.restTemplate = RestTemplateUtils.buildRestTemplate(
            http.getConnectTimeout(), http.getReadTimeout(),
            StandardCharsets.UTF_8, httpMessageConverter
        );
        this.supervisorRegistry = supervisorRegistry;
        this.supervisorRpcService = supervisorRpcService;
    }

    // ------------------------------------------------------------supervisors

    public List<SupervisorInfoResponse> supervisors() throws Exception {
        List<Supervisor> list = supervisorRegistry.getRegisteredServers();
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }

        return list.stream()
            .sorted(Comparator.comparing(e -> e.equals(Supervisor.current()) ? 0 : 1))
            .map(e -> CompletableFuture.supplyAsync(() -> convert(e)))
            .collect(Collectors.toList())
            .stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
    }

    // ------------------------------------------------------------private methods

    private SupervisorInfoResponse convert(Supervisor supervisor) {
        SupervisorInfoResponse response = new SupervisorInfoResponse();
        response.setHost(supervisor.getHost());
        response.setPort(supervisor.getPort());

        if (supervisor.equals(Supervisor.current())) {
            response.setPingTime(0L);
            response.setIsWorker(supervisorRpcService.isWorker());
        } else {
            String url = String.format(IS_WORKER_URL_PATH, supervisor.getHost(), supervisor.getPort());
            try {
                long start = System.currentTimeMillis();
                Boolean result = restTemplate.getForObject(url, Boolean.class);
                response.setPingTime(System.currentTimeMillis() - start);
                response.setIsWorker(result);
            } catch (Throwable e) {
                LOG.warn("Ping occur error: {} {}", supervisor, e.getMessage());
            }
        }

        return response;
    }

}
