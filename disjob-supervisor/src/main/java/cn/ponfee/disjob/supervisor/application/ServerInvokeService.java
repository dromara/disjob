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

package cn.ponfee.disjob.supervisor.application;

import cn.ponfee.disjob.common.base.RetryTemplate;
import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.concurrent.MultithreadExecutors;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.util.Numbers;
import cn.ponfee.disjob.core.base.*;
import cn.ponfee.disjob.core.exception.AuthenticationException;
import cn.ponfee.disjob.core.exception.KeyExistsException;
import cn.ponfee.disjob.core.exception.KeyNotExistsException;
import cn.ponfee.disjob.core.param.supervisor.EventParam;
import cn.ponfee.disjob.core.param.worker.ConfigureWorkerParam;
import cn.ponfee.disjob.core.param.worker.ConfigureWorkerParam.Action;
import cn.ponfee.disjob.core.param.worker.GetMetricsParam;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.registry.rpc.DestinationServerRestProxy;
import cn.ponfee.disjob.registry.rpc.DestinationServerRestProxy.DestinationServerInvoker;
import cn.ponfee.disjob.supervisor.application.converter.ServerMetricsConverter;
import cn.ponfee.disjob.supervisor.application.request.ConfigureAllWorkerRequest;
import cn.ponfee.disjob.supervisor.application.request.ConfigureOneWorkerRequest;
import cn.ponfee.disjob.supervisor.application.response.SupervisorMetricsResponse;
import cn.ponfee.disjob.supervisor.application.response.WorkerMetricsResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Invoke remote server service
 *
 * @author Ponfee
 */
@Service
public class ServerInvokeService extends SingletonClassConstraint {
    private static final Logger LOG = LoggerFactory.getLogger(ServerInvokeService.class);

    private final SupervisorRegistry supervisorRegistry;
    private final Supervisor.Current currentSupervisor;
    private final DestinationServerInvoker<SupervisorRpcService, Supervisor> supervisorRpcClient;
    private final DestinationServerInvoker<WorkerRpcService, Worker> workerRpcClient;

    public ServerInvokeService(SupervisorRegistry supervisorRegistry,
                               Supervisor.Current currentSupervisor,
                               @Value("${server.servlet.context-path:/}") String contextPath,
                               SupervisorRpcService localSupervisorRpcProvider,
                               @Qualifier(JobConstants.SPRING_BEAN_NAME_REST_TEMPLATE) RestTemplate restTemplate,
                               DestinationServerInvoker<WorkerRpcService, Worker> workerRpcClient) {
        this.supervisorRegistry = supervisorRegistry;
        this.currentSupervisor = currentSupervisor;
        this.supervisorRpcClient = DestinationServerRestProxy.create(
            SupervisorRpcService.class,
            localSupervisorRpcProvider,
            currentSupervisor,
            supervisor -> contextPath,
            restTemplate,
            RetryProperties.of(0, 0)
        );
        this.workerRpcClient = workerRpcClient;
    }

    // ------------------------------------------------------------public methods

    public List<SupervisorMetricsResponse> supervisors() throws Exception {
        List<Supervisor> list = supervisorRegistry.getRegisteredServers();
        list = Collects.sorted(list, Comparator.comparing(e -> e.equals(currentSupervisor) ? 0 : 1));
        return MultithreadExecutors.call(list, this::getSupervisorMetrics, ThreadPoolExecutors.commonThreadPool());
    }

    public List<WorkerMetricsResponse> workers(String group, String worker) {
        if (StringUtils.isNotBlank(worker)) {
            String[] array = worker.trim().split(":");
            String host = array[0].trim();
            int port = Numbers.toInt(array[1].trim(), -1);
            WorkerMetricsResponse metrics = getWorkerMetrics(new Worker(group, "", host, port));
            return StringUtils.isBlank(metrics.getWorkerId()) ? Collections.emptyList() : Collections.singletonList(metrics);
        } else {
            List<Worker> list = supervisorRegistry.getDiscoveredServers(group);
            list = Collects.sorted(list, Comparator.comparing(e -> e.equals(Worker.current()) ? 0 : 1));
            return MultithreadExecutors.call(list, this::getWorkerMetrics, ThreadPoolExecutors.commonThreadPool());
        }
    }

    public void configureOneWorker(ConfigureOneWorkerRequest req) {
        Worker worker = req.toWorker();
        if (req.getAction() == Action.ADD_WORKER) {
            List<Worker> workers = supervisorRegistry.getDiscoveredServers(req.getGroup());
            if (workers != null && workers.stream().anyMatch(worker::sameWorker)) {
                throw new KeyExistsException("Worker already registered: " + worker);
            }
            verifyWorkerSignature(worker);
            // add worker to this group
            req.setData(req.getGroup());
        } else {
            List<Worker> workers = getDiscoveredWorkers(req.getGroup());
            if (!workers.contains(worker)) {
                throw new KeyNotExistsException("Not found worker: " + worker);
            }
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

    public void publishOtherSupervisors(EventParam eventParam) {
        try {
            List<Supervisor> supervisors = supervisorRegistry.getRegisteredServers()
                .stream()
                .filter(e -> !currentSupervisor.sameSupervisor(e))
                .collect(Collectors.toList());
            MultithreadExecutors.run(
                supervisors,
                supervisor -> publishSupervisor(supervisor, eventParam),
                ThreadPoolExecutors.commonThreadPool()
            );
        } catch (Exception e) {
            LOG.error("Publish all supervisor error.", e);
        }
    }

    // ------------------------------------------------------------private methods

    private SupervisorMetricsResponse getSupervisorMetrics(Supervisor supervisor) {
        SupervisorMetrics metrics = null;
        Long pingTime = null;
        try {
            long start = System.currentTimeMillis();
            metrics = supervisorRpcClient.call(supervisor, SupervisorRpcService::metrics);
            pingTime = System.currentTimeMillis() - start;
        } catch (Throwable e) {
            LOG.warn("Ping supervisor occur error: {} {}", supervisor, e.getMessage());
        }

        SupervisorMetricsResponse response;
        if (metrics == null) {
            response = new SupervisorMetricsResponse();
        } else {
            response = ServerMetricsConverter.INSTANCE.convert(metrics);
        }

        response.setHost(supervisor.getHost());
        response.setPort(supervisor.getPort());
        response.setPingTime(pingTime);
        return response;
    }

    private WorkerMetricsResponse getWorkerMetrics(Worker worker) {
        WorkerMetrics metrics = null;
        Long pingTime = null;
        String group = worker.getGroup();
        GetMetricsParam param = buildGetMetricsParam(group);
        try {
            long start = System.currentTimeMillis();
            metrics = workerRpcClient.call(worker, client -> client.metrics(param));
            pingTime = System.currentTimeMillis() - start;
        } catch (Throwable e) {
            LOG.warn("Ping worker occur error: {} {}", worker, e.getMessage());
        }

        WorkerMetricsResponse response;
        if (metrics == null || !SchedGroupService.verifyWorkerSignatureToken(metrics.getSignature(), group)) {
            response = new WorkerMetricsResponse(worker.getWorkerId());
        } else {
            response = ServerMetricsConverter.INSTANCE.convert(metrics);
        }

        response.setHost(worker.getHost());
        response.setPort(worker.getPort());
        response.setPingTime(pingTime);
        return response;
    }

    private List<Worker> getDiscoveredWorkers(String group) {
        List<Worker> list = supervisorRegistry.getDiscoveredServers(group);
        if (CollectionUtils.isEmpty(list)) {
            throw new KeyNotExistsException("Group '" + group + "' not exists workers.");
        }
        return list;
    }

    private void verifyWorkerSignature(Worker worker) {
        String group = worker.getGroup();
        GetMetricsParam param = buildGetMetricsParam(group);
        WorkerMetrics metrics = workerRpcClient.call(worker, client -> client.metrics(param));
        if (!SchedGroupService.verifyWorkerSignatureToken(metrics.getSignature(), group)) {
            throw new AuthenticationException("Worker authenticated failed: " + worker);
        }
    }

    private void configureWorker(Worker worker, Action action, String data) {
        ConfigureWorkerParam param = new ConfigureWorkerParam(SchedGroupService.createSupervisorAuthenticationToken(worker.getGroup()));
        param.setAction(action);
        param.setData(data);
        workerRpcClient.invoke(worker, client -> client.configureWorker(param));
    }

    private void publishSupervisor(Supervisor supervisor, EventParam param) {
        RetryTemplate.executeQuietly(() -> supervisorRpcClient.invoke(supervisor, client -> client.publish(param)), 1, 2000);
    }

    private GetMetricsParam buildGetMetricsParam(String group) {
        return new GetMetricsParam(SchedGroupService.createSupervisorAuthenticationToken(group), group);
    }

}
