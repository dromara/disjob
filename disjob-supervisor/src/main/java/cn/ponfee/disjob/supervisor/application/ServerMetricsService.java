/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application;

import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.concurrent.MultithreadExecutors;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.spring.RestTemplateUtils;
import cn.ponfee.disjob.core.base.*;
import cn.ponfee.disjob.core.exception.KeyNotExistsException;
import cn.ponfee.disjob.core.param.worker.ConfigureWorkerParam;
import cn.ponfee.disjob.core.param.worker.ConfigureWorkerParam.Action;
import cn.ponfee.disjob.core.param.worker.GetMetricsParam;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.supervisor.application.converter.ServerMetricsConverter;
import cn.ponfee.disjob.supervisor.application.request.ConfigureAllWorkerRequest;
import cn.ponfee.disjob.supervisor.application.request.ConfigureOneWorkerRequest;
import cn.ponfee.disjob.supervisor.application.response.SupervisorMetricsResponse;
import cn.ponfee.disjob.supervisor.application.response.WorkerMetricsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

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
    private static final String WORKER_CONFIGURE_URL = "http://%s:%d/" + WorkerRpcService.PREFIX_PATH + "worker/configure";

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

    // ------------------------------------------------------------public methods

    public List<SupervisorMetricsResponse> supervisors() throws Exception {
        List<Supervisor> list = supervisorRegistry.getRegisteredServers();
        list = Collects.sorted(list, Comparator.comparing(e -> e.equals(Supervisor.current()) ? 0 : 1));
        return MultithreadExecutors.call(list, this::getMetrics, ThreadPoolExecutors.commonThreadPool());
    }

    public List<WorkerMetricsResponse> workers(String group) {
        List<Worker> list = supervisorRegistry.getDiscoveredServers(group);
        list = Collects.sorted(list, Comparator.comparing(e -> e.equals(Worker.current()) ? 0 : 1));
        return MultithreadExecutors.call(list, this::getMetrics, ThreadPoolExecutors.commonThreadPool());
    }

    public void configureOneWorker(ConfigureOneWorkerRequest req) {
        List<Worker> workers = getDiscoveredWorkers(req.getGroup());
        Worker worker = req.toWorker();
        if (!workers.contains(worker)) {
            throw new KeyNotExistsException("Not found worker: " + worker);
        }
        configureWorker(worker, req.getAction(), req.getData());
    }

    public void configureAllWorker(ConfigureAllWorkerRequest req) {
        List<Worker> workers = getDiscoveredWorkers(req.getGroup());
        MultithreadExecutors.run(
            workers,
            worker -> configureWorker(worker, req.getAction(), req.getData()),
            ThreadPoolExecutors.commonThreadPool()
        );
    }

    // ------------------------------------------------------------private methods

    private SupervisorMetricsResponse getMetrics(Supervisor supervisor) {
        SupervisorMetrics metrics = null;
        Long pingTime = null;
        String url = String.format(SUPERVISOR_METRICS_URL, supervisor.getHost(), supervisor.getPort());
        try {
            long start = System.currentTimeMillis();
            metrics = RestTemplateUtils.invokeRpc(restTemplate, url, HttpMethod.GET, SupervisorMetrics.class, null);
            pingTime = System.currentTimeMillis() - start;
        } catch (Throwable e) {
            LOG.warn("Ping supervisor occur error: {} {}", supervisor, e.getMessage());
        }

        SupervisorMetricsResponse response = (metrics == null) ?
            new SupervisorMetricsResponse() :
            ServerMetricsConverter.INSTANCE.convert(metrics);

        response.setHost(supervisor.getHost());
        response.setPort(supervisor.getPort());
        response.setPingTime(pingTime);
        return response;
    }

    private WorkerMetricsResponse getMetrics(Worker worker) {
        WorkerMetrics metrics = null;
        Long pingTime = null;
        String url = String.format(WORKER_METRICS_URL, worker.getHost(), worker.getPort());
        GetMetricsParam param = new GetMetricsParam(supervisorToken(worker.getGroup()));
        try {
            long start = System.currentTimeMillis();
            metrics = RestTemplateUtils.invokeRpc(restTemplate, url, HttpMethod.GET, WorkerMetrics.class, null, param);
            pingTime = System.currentTimeMillis() - start;
        } catch (Throwable e) {
            LOG.warn("Ping worker occur error: {} {}", worker, e.getMessage());
        }

        WorkerMetricsResponse response = (metrics == null) ?
            new WorkerMetricsResponse() :
            ServerMetricsConverter.INSTANCE.convert(metrics);

        response.setHost(worker.getHost());
        response.setPort(worker.getPort());
        response.setWorkerId(worker.getWorkerId());
        response.setPingTime(pingTime);
        return response;
    }

    public List<Worker> getDiscoveredWorkers(String group) {
        List<Worker> list = supervisorRegistry.getDiscoveredServers(group);
        if (CollectionUtils.isEmpty(list)) {
            throw new KeyNotExistsException("Not exists workers: " + group);
        }
        return list;
    }

    private void configureWorker(Worker worker, Action action, String data) {
        String url = String.format(WORKER_CONFIGURE_URL, worker.getHost(), worker.getPort());
        ConfigureWorkerParam param = new ConfigureWorkerParam(supervisorToken(worker.getGroup()));
        param.setAction(action);
        param.setData(data);
        RestTemplateUtils.invokeRpc(restTemplate, url, HttpMethod.POST, Void.class, null, param);
    }

    private static String supervisorToken(String group) {
        return SchedGroupService.getGroup(group).getSupervisorToken();
    }

}
