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
import cn.ponfee.disjob.core.dto.worker.ConfigureWorkerParam;
import cn.ponfee.disjob.core.dto.worker.ConfigureWorkerParam.Action;
import cn.ponfee.disjob.core.dto.worker.GetMetricsParam;
import cn.ponfee.disjob.core.exception.AuthenticationException;
import cn.ponfee.disjob.registry.Registry;
import cn.ponfee.disjob.registry.rpc.DestinationServerRestProxy;
import cn.ponfee.disjob.supervisor.application.converter.ServerMetricsConverter;
import cn.ponfee.disjob.supervisor.application.request.ConfigureAllWorkerRequest;
import cn.ponfee.disjob.supervisor.application.request.ConfigureOneWorkerRequest;
import cn.ponfee.disjob.supervisor.application.response.SupervisorMetricsResponse;
import cn.ponfee.disjob.supervisor.application.response.WorkerMetricsResponse;
import cn.ponfee.disjob.supervisor.base.ExtendedSupervisorRpcService;
import cn.ponfee.disjob.supervisor.base.OperationEventType;
import cn.ponfee.disjob.supervisor.base.SupervisorMetrics;
import cn.ponfee.disjob.supervisor.component.WorkerClient;
import cn.ponfee.disjob.supervisor.exception.KeyExistsException;
import cn.ponfee.disjob.supervisor.exception.KeyNotExistsException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static cn.ponfee.disjob.common.base.Symbol.Str.COLON;

/**
 * Invoke remote server service
 *
 * @author Ponfee
 */
@Service
public class ServerInvokeService extends SingletonClassConstraint {
    private static final Logger LOG = LoggerFactory.getLogger(ServerInvokeService.class);

    private final WorkerClient workerClient;
    private final Registry<Supervisor> supervisorRegistry;
    private final Supervisor.Local localSupervisor;
    private final DestinationServerRestProxy<ExtendedSupervisorRpcService, Supervisor> supervisorRpcProxy;

    public ServerInvokeService(WorkerClient workerClient,
                               Registry<Supervisor> supervisorRegistry,
                               Supervisor.Local localSupervisor,
                               ExtendedSupervisorRpcService localSupervisorRpcProvider,
                               @Qualifier(JobConstants.SPRING_BEAN_NAME_REST_TEMPLATE) RestTemplate restTemplate) {
        this.workerClient = workerClient;
        this.supervisorRegistry = supervisorRegistry;
        this.localSupervisor = localSupervisor;
        this.supervisorRpcProxy = DestinationServerRestProxy.of(
            ExtendedSupervisorRpcService.class,
            localSupervisorRpcProvider,
            localSupervisor,
            restTemplate,
            RetryProperties.none()
        );
    }

    // ------------------------------------------------------------public methods

    public List<SupervisorMetricsResponse> supervisors() {
        List<Supervisor> list = supervisorRegistry.getRegisteredServers();
        list = Collects.sorted(list, Comparator.comparing(e -> localSupervisor.equals(e) ? 0 : 1));
        return MultithreadExecutors.call(list, this::getSupervisorMetrics, ThreadPoolExecutors.commonThreadPool());
    }

    public List<WorkerMetricsResponse> workers(String group) {
        List<Worker> list = workerClient.getAliveWorkers(group);
        // 当前Supervisor同时也是Worker时，此Worker排到最前面
        list = Collects.sorted(list, Comparator.comparing(e -> e.equals(Worker.local()) ? 0 : 1));
        return MultithreadExecutors.call(list, this::getWorkerMetrics, ThreadPoolExecutors.commonThreadPool());
    }

    public WorkerMetricsResponse worker(String group, String worker) {
        String[] array;
        if (StringUtils.isBlank(worker) || (array = worker.split(COLON, 2)).length != 2) {
            return null;
        }
        String host = array[0];
        Integer port = Numbers.toWrapInt(StringUtils.trim(array[1]));
        if (StringUtils.isBlank(host) || port == null) {
            return null;
        }
        WorkerMetricsResponse metrics = getWorkerMetrics(new Worker(group, "-", host.trim(), port));
        return StringUtils.isBlank(metrics.getWorkerId()) ? null : metrics;
    }

    public void configureOneWorker(ConfigureOneWorkerRequest req) {
        Worker worker = req.toWorker();
        if (req.getAction() == Action.ADD_WORKER) {
            List<Worker> workers = workerClient.getAliveWorkers(req.getGroup());
            if (workers != null && workers.stream().anyMatch(worker::matches)) {
                throw new KeyExistsException("Worker already registered: " + worker);
            }
        } else {
            List<Worker> workers = getRequiredAliveWorkers(req.getGroup());
            if (!workers.contains(worker)) {
                throw new KeyNotExistsException("Not found worker: " + worker);
            }
        }

        configureWorker(worker, req.getAction(), req.getData());
    }

    public void configureAllWorker(ConfigureAllWorkerRequest req) {
        List<Worker> workers = getRequiredAliveWorkers(req.getGroup());
        Consumer<Worker> action = worker -> configureWorker(worker, req.getAction(), req.getData());
        MultithreadExecutors.run(workers, action, ThreadPoolExecutors.commonThreadPool());
    }

    public void publishOperationEvent(OperationEventType eventType, String eventData, boolean includeSelf) {
        try {
            List<Supervisor> supervisors = supervisorRegistry.getRegisteredServers()
                .stream()
                .filter(e -> includeSelf || !localSupervisor.equals(e))
                .collect(Collectors.toList());
            final Date eventTime = new Date();
            Consumer<Supervisor> action = supervisor -> publishOperationEvent(supervisor, eventType, eventTime, eventData);
            MultithreadExecutors.run(supervisors, action, ThreadPoolExecutors.commonThreadPool());
        } catch (Exception e) {
            LOG.error("Publish operation event to other supervisor error.", e);
        }
    }

    // ------------------------------------------------------------private methods

    private SupervisorMetricsResponse getSupervisorMetrics(Supervisor supervisor) {
        SupervisorMetrics metrics = null;
        Long responseTime = null;
        try {
            long start = System.currentTimeMillis();
            metrics = supervisorRpcProxy.destination(supervisor).getMetrics();
            responseTime = System.currentTimeMillis() - start;
        } catch (Throwable e) {
            LOG.warn("Gets supervisor metrics occur error: {} {}", supervisor, e.getMessage());
        }

        SupervisorMetricsResponse response;
        if (metrics == null) {
            response = new SupervisorMetricsResponse();
        } else {
            response = ServerMetricsConverter.INSTANCE.convert(metrics);
        }

        response.setHost(supervisor.getHost());
        response.setPort(supervisor.getPort());
        response.setResponseTime(responseTime);
        return response;
    }

    private WorkerMetricsResponse getWorkerMetrics(Worker worker) {
        WorkerMetrics metrics = null;
        Long responseTime = null;
        GetMetricsParam param = GetMetricsParam.of(worker.getGroup());
        try {
            long start = System.currentTimeMillis();
            metrics = workerClient.destination(worker).getMetrics(param);
            responseTime = System.currentTimeMillis() - start;
        } catch (Throwable e) {
            LOG.warn("Gets worker metrics occur error: {} {}", worker, e.getMessage());
        }

        WorkerMetricsResponse response;
        if (metrics == null || !Supervisor.local().verifyWorkerSignatureToken(worker.getGroup(), metrics.getSignature())) {
            response = new WorkerMetricsResponse();
        } else {
            response = ServerMetricsConverter.INSTANCE.convert(metrics);
        }
        response.setHost(worker.getHost());
        response.setPort(worker.getPort());
        response.setResponseTime(responseTime);
        return response;
    }

    private List<Worker> getRequiredAliveWorkers(String group) {
        List<Worker> workers = workerClient.getAliveWorkers(group);
        if (CollectionUtils.isEmpty(workers)) {
            throw new KeyNotExistsException("Group '" + group + "' not exists workers.");
        }
        return workers;
    }

    private void verifyWorkerSignature(Worker worker) {
        GetMetricsParam param = GetMetricsParam.of(worker.getGroup());
        WorkerMetrics metrics = workerClient.destination(worker).getMetrics(param);
        if (!Supervisor.local().verifyWorkerSignatureToken(worker.getGroup(), metrics.getSignature())) {
            throw new AuthenticationException("Worker authenticated failed: " + worker);
        }
    }

    private void configureWorker(Worker worker, Action action, String data) {
        if (action == Action.ADD_WORKER) {
            verifyWorkerSignature(worker);
        }
        ConfigureWorkerParam param = ConfigureWorkerParam.of(worker.getGroup(), action, data);
        workerClient.destination(worker).configureWorker(param);
    }

    private void publishOperationEvent(Supervisor supervisor, OperationEventType eventType, Date eventTime, String eventData) {
        RetryTemplate.executeQuietly(
            () -> supervisorRpcProxy.destination(supervisor).subscribeOperationEvent(eventType, eventTime, eventData), 1, 2000
        );
    }

}
