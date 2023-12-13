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
import cn.ponfee.disjob.core.base.*;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.supervisor.application.converter.ServerMetricsConverter;
import cn.ponfee.disjob.supervisor.application.response.SupervisorMetricsResponse;
import cn.ponfee.disjob.supervisor.application.response.WorkerMetricsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
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
@Service
public class ServerMetricsService extends SingletonClassConstraint {
    private static final Logger LOG = LoggerFactory.getLogger(ServerMetricsService.class);

    private static final String SUPERVISOR_METRICS_URL = "http://%s:%d/" + SupervisorRpcService.PREFIX_PATH + "metrics";
    private static final String WORKER_METRICS_URL = "http://%s:%d/" + WorkerRpcService.PREFIX_PATH + "metrics";

    private final RestTemplate restTemplate;
    private final SupervisorRegistry supervisorRegistry;

    public ServerMetricsService(HttpProperties http,
                                ObjectMapper objectMapper,
                                SupervisorRegistry supervisorRegistry) {
        MappingJackson2HttpMessageConverter httpMessageConverter = new MappingJackson2HttpMessageConverter();
        httpMessageConverter.setObjectMapper(objectMapper);
        RestTemplateUtils.extensionSupportedMediaTypes(httpMessageConverter);
        this.restTemplate = RestTemplateUtils.buildRestTemplate(
            http.getConnectTimeout(), http.getReadTimeout(),
            StandardCharsets.UTF_8, httpMessageConverter
        );
        this.supervisorRegistry = supervisorRegistry;
    }

    // ------------------------------------------------------------supervisors

    public List<SupervisorMetricsResponse> supervisors() throws Exception {
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

    public List<WorkerMetricsResponse> workers(String group) {
        List<Worker> list = supervisorRegistry.getDiscoveredServers(group);
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }

        return list.stream()
            .sorted(Comparator.comparing(e -> e.equals(Worker.current()) ? 0 : 1))
            .map(e -> CompletableFuture.supplyAsync(() -> convert(e)))
            .collect(Collectors.toList())
            .stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
    }

    // ------------------------------------------------------------private methods

    private SupervisorMetricsResponse convert(Supervisor supervisor) {
        SupervisorMetrics metrics = null;
        Long pingTime = null;
        String url = String.format(SUPERVISOR_METRICS_URL, supervisor.getHost(), supervisor.getPort());
        try {
            long start = System.currentTimeMillis();
            metrics = restTemplate.getForObject(url, SupervisorMetrics.class);
            pingTime = System.currentTimeMillis() - start;
        } catch (Throwable e) {
            LOG.warn("Ping supervisor occur error: {} {}", supervisor, e.getMessage());
        }

        SupervisorMetricsResponse response = (metrics == null)
                                            ? new SupervisorMetricsResponse()
                                            : ServerMetricsConverter.INSTANCE.convert(metrics);
        response.setHost(supervisor.getHost());
        response.setPort(supervisor.getPort());
        response.setPingTime(pingTime);
        return response;
    }

    private WorkerMetricsResponse convert(Worker worker) {
        WorkerMetrics metrics = null;
        Long pingTime = null;
        String url = String.format(WORKER_METRICS_URL, worker.getHost(), worker.getPort());
        try {
            long start = System.currentTimeMillis();
            metrics = restTemplate.getForObject(url, WorkerMetrics.class);
            pingTime = System.currentTimeMillis() - start;
        } catch (Throwable e) {
            LOG.warn("Ping worker occur error: {} {}", worker, e.getMessage());
        }

        WorkerMetricsResponse response = (metrics == null)
                                        ? new WorkerMetricsResponse()
                                        : ServerMetricsConverter.INSTANCE.convert(metrics);
        response.setHost(worker.getHost());
        response.setPort(worker.getPort());
        response.setWorkerId(worker.getWorkerId());
        response.setPingTime(pingTime);
        return response;
    }

}
