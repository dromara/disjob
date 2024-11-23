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

package cn.ponfee.disjob.supervisor.component;

import cn.ponfee.disjob.common.exception.Throwables.ThrowingConsumer;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingFunction;
import cn.ponfee.disjob.common.spring.TransactionUtils;
import cn.ponfee.disjob.core.base.*;
import cn.ponfee.disjob.core.dto.worker.ExistsTaskParam;
import cn.ponfee.disjob.core.dto.worker.SplitJobParam;
import cn.ponfee.disjob.core.dto.worker.SplitJobResult;
import cn.ponfee.disjob.core.dto.worker.VerifyJobParam;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import cn.ponfee.disjob.dispatch.TaskDispatcher;
import cn.ponfee.disjob.registry.Discovery;
import cn.ponfee.disjob.registry.rpc.DestinationServerRestProxy;
import cn.ponfee.disjob.registry.rpc.DestinationServerRestProxy.DestinationServerClient;
import cn.ponfee.disjob.registry.rpc.DiscoveryServerRestProxy;
import cn.ponfee.disjob.registry.rpc.DiscoveryServerRestProxy.GroupedServerClient;
import cn.ponfee.disjob.supervisor.base.ModelConverter;
import cn.ponfee.disjob.supervisor.model.SchedJob;
import cn.ponfee.disjob.supervisor.model.SchedTask;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Predicate;

import static cn.ponfee.disjob.core.base.JobConstants.SPRING_BEAN_NAME_REST_TEMPLATE;

/**
 * Worker client
 *
 * @author Ponfee
 */
@Component
public class WorkerClient {

    private static final Logger LOG = LoggerFactory.getLogger(WorkerClient.class);

    private final Discovery<Worker> discoverWorker;
    private final TaskDispatcher taskDispatcher;
    private final GroupedServerClient<WorkerRpcService> groupedClient;
    private final DestinationServerClient<WorkerRpcService, Worker> destinationClient;

    public WorkerClient(Discovery<Worker> discoverWorker,
                        TaskDispatcher taskDispatcher,
                        Supervisor.Local localSupervisor,
                        RetryProperties retry,
                        @Qualifier(SPRING_BEAN_NAME_REST_TEMPLATE) RestTemplate restTemplate,
                        @Nullable WorkerRpcService workerRpcProvider,
                        @Nullable Worker.Local localWorker) {
        this.discoverWorker = discoverWorker;
        this.taskDispatcher = taskDispatcher;

        retry.check();
        Predicate<String> serverGroupMatcher = localWorker != null ? localWorker::equalsGroup : group -> false;
        this.groupedClient = DiscoveryServerRestProxy.create(
            WorkerRpcService.class, workerRpcProvider, serverGroupMatcher, discoverWorker, restTemplate, retry
        );

        Function<Worker, String> workerContextPath = worker -> localSupervisor.getWorkerContextPath(worker.getGroup());
        this.destinationClient = DestinationServerRestProxy.create(
            WorkerRpcService.class, workerRpcProvider, localWorker, workerContextPath, restTemplate, RetryProperties.none()
        );
    }

    public List<Worker> getDiscoveredWorkers(String group) {
        return discoverWorker.getDiscoveredServers(group);
    }

    public boolean hasNotDiscoveredWorkers(String group) {
        return CollectionUtils.isEmpty(getDiscoveredWorkers(group));
    }

    public boolean hasNotDiscoveredWorkers() {
        return !discoverWorker.hasDiscoveredServers();
    }

    public boolean isAliveWorker(Worker worker) {
        return worker != null && discoverWorker.isDiscoveredServer(worker);
    }

    public boolean hasAliveExecutingTasks(List<SchedTask> tasks) {
        return CollectionUtils.isNotEmpty(tasks)
            && tasks.stream().filter(SchedTask::isExecuting).anyMatch(e -> isAliveWorker(e.worker()));
    }

    public boolean shouldRedispatch(SchedTask task) {
        if (!task.isWaiting()) {
            return false;
        }

        Worker worker = task.worker();
        if (!isAliveWorker(worker)) {
            return true;
        }

        ExistsTaskParam param = ExistsTaskParam.of(worker, task.getTaskId());
        try {
            // `WorkerRpcService#existsTask`：判断任务是否在线程池中，如果不在则可能是没有分发成功，需要重新分发
            return !destinationClient.call(worker, service -> service.existsTask(param));
        } catch (Throwable e) {
            LOG.error("Invoke worker exists task error: " + worker, e);
            // 若调用异常(如请求超时)，则默认为Worker已存在该task，本次不做处理，等下一次扫描时再判断是否要重新分发任务
            return false;
        }
    }

    public void verifyJob(SchedJob job) throws JobException {
        Assert.hasText(job.getJobExecutor(), "Job executor cannot be blank.");
        CoreUtils.checkClobMaximumLength(job.getJobExecutor(), "Job executor");
        CoreUtils.checkClobMaximumLength(job.getJobParam(), "Job param");
        JobType.of(job.getJobType());
        RouteStrategy.of(job.getRouteStrategy());

        VerifyJobParam param = ModelConverter.toVerifyJobParam(job);
        groupedClient.invoke(job.getGroup(), client -> client.verifyJob(param));
    }

    public List<SchedTask> splitJob(String group, long instanceId, SplitJobParam param,
                                    LongSupplier idGenerator, int maximumSplitTaskSize) throws JobException {
        List<Worker> workers = getDiscoveredWorkers(group);
        Assert.state(!workers.isEmpty(), () -> "Not discovered worker for split job: " + group);
        int wCount = workers.size();

        param.setWorkerCount(wCount);
        SplitJobResult result = groupedClient.call(group, client -> client.splitJob(param));
        List<String> taskParams = result.getTaskParams();
        taskParams.forEach(e -> CoreUtils.checkClobMaximumLength(e, "Split task param"));

        int tCount = taskParams.size();
        boolean isBroadcast = param.getRouteStrategy().isBroadcast();
        if (isBroadcast) {
            Assert.state(tCount == wCount, () -> "Illegal broadcast split task size: " + tCount + "!=" + wCount);
        } else {
            Assert.state(0 < tCount && tCount <= maximumSplitTaskSize, () -> "Illegal split task size: " + tCount);
        }

        List<SchedTask> tasks = new ArrayList<>(tCount);
        for (int i = 0; i < tCount; i++) {
            String worker = isBroadcast ? workers.get(i).serialize() : null;
            tasks.add(SchedTask.of(taskParams.get(i), idGenerator.getAsLong(), instanceId, i + 1, tCount, worker));
        }
        return tasks;
    }

    public <R, E extends Throwable> R call(Worker worker, ThrowingFunction<WorkerRpcService, R, E> function) throws E {
        return destinationClient.call(worker, function);
    }

    public <E extends Throwable> void invoke(Worker worker, ThrowingConsumer<WorkerRpcService, E> consumer) throws E {
        destinationClient.call(worker, consumer.toFunction(null));
    }

    public boolean dispatch(List<ExecuteTaskParam> tasks) {
        Assert.isTrue(TransactionUtils.isNotDoInTransaction(), "Dispatch task cannot in transaction.");
        return taskDispatcher.dispatch(tasks);
    }

    public boolean dispatch(String group, List<ExecuteTaskParam> tasks) {
        Assert.isTrue(TransactionUtils.isNotDoInTransaction(), "Dispatch task cannot in transaction.");
        return taskDispatcher.dispatch(group, tasks);
    }

}
