/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

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
import cn.ponfee.disjob.registry.rpc.ServerRestProxy;
import cn.ponfee.disjob.registry.rpc.ServerRestProxy.DesignatedServerInvoker;
import cn.ponfee.disjob.supervisor.application.converter.ServerMetricsConverter;
import cn.ponfee.disjob.supervisor.application.request.ConfigureAllWorkerRequest;
import cn.ponfee.disjob.supervisor.application.request.ConfigureOneWorkerRequest;
import cn.ponfee.disjob.supervisor.application.response.SupervisorMetricsResponse;
import cn.ponfee.disjob.supervisor.application.response.WorkerMetricsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Server info service
 *
 * @author Ponfee
 */
@Service
public class ServerInvokeService extends SingletonClassConstraint {
    private static final Logger LOG = LoggerFactory.getLogger(ServerInvokeService.class);

    private final DesignatedServerInvoker<SupervisorRpcService> invokeSupervisor;
    private final DesignatedServerInvoker<WorkerRpcService> invokeWorker;
    private final SupervisorRegistry supervisorRegistry;

    public ServerInvokeService(HttpProperties http,
                               SupervisorRegistry supervisorRegistry,
                               SupervisorRpcService supervisorRpcServiceProvider,
                               @Nullable WorkerRpcService workerRpcServiceProvider,
                               @Nullable ObjectMapper objectMapper) {
        RetryProperties retry = RetryProperties.of(0, 0);
        this.invokeSupervisor = ServerRestProxy.create(
            SupervisorRpcService.class, supervisorRpcServiceProvider, Supervisor.current(), http, retry, objectMapper
        );
        this.invokeWorker = ServerRestProxy.create(
            WorkerRpcService.class, workerRpcServiceProvider, Worker.current(), http, retry, objectMapper
        );
        this.supervisorRegistry = supervisorRegistry;
    }

    // ------------------------------------------------------------public methods

    public List<SupervisorMetricsResponse> supervisors() throws Exception {
        List<Supervisor> list = supervisorRegistry.getRegisteredServers();
        list = Collects.sorted(list, Comparator.comparing(e -> e.equals(Supervisor.current()) ? 0 : 1));
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
                .filter(e -> !Supervisor.current().sameSupervisor(e))
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
            metrics = invokeSupervisor.invoke(supervisor, SupervisorRpcService::metrics);
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
        GetMetricsParam param = new GetMetricsParam(SchedGroupService.createSupervisorAuthenticationToken(group), group);
        try {
            long start = System.currentTimeMillis();
            metrics = invokeWorker.invoke(worker, client -> client.metrics(param));
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
        GetMetricsParam param = new GetMetricsParam(SchedGroupService.createSupervisorAuthenticationToken(group), group);
        WorkerMetrics metrics = invokeWorker.invoke(worker, client -> client.metrics(param));
        if (!SchedGroupService.verifyWorkerSignatureToken(metrics.getSignature(), group)) {
            throw new AuthenticationException("Worker authenticated failed: " + worker);
        }
    }

    private void configureWorker(Worker worker, Action action, String data) {
        ConfigureWorkerParam param = new ConfigureWorkerParam(SchedGroupService.createSupervisorAuthenticationToken(worker.getGroup()));
        param.setAction(action);
        param.setData(data);
        invokeWorker.invokeWithoutResult(worker, client -> client.configureWorker(param));
    }

    private void publishSupervisor(Supervisor supervisor, EventParam param) {
        RetryTemplate.executeQuietly(() -> invokeSupervisor.invokeWithoutResult(supervisor, client -> client.publish(param)), 1, 2000);
    }

}
