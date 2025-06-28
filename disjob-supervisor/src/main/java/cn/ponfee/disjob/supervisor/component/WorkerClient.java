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

import cn.ponfee.disjob.common.base.IdGenerator;
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
import cn.ponfee.disjob.registry.rpc.DiscoveryGroupedServerRestProxy;
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
import java.util.function.Predicate;

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
    private final DiscoveryGroupedServerRestProxy<WorkerRpcService> groupedProxy;
    private final DestinationServerRestProxy<WorkerRpcService, Worker> destinationProxy;

    public WorkerClient(Discovery<Worker> discoverWorker,
                        TaskDispatcher taskDispatcher,
                        RetryProperties retry,
                        @Qualifier(JobConstants.SPRING_BEAN_NAME_REST_TEMPLATE) RestTemplate restTemplate,
                        @Nullable WorkerRpcService workerRpcProvider,
                        @Nullable Worker.Local localWorker) {
        this.discoverWorker = discoverWorker;
        this.taskDispatcher = taskDispatcher;

        retry.check();
        Predicate<String> localGroupMatcher = localWorker != null ? localWorker::equalsGroup : group -> false;
        this.groupedProxy = DiscoveryGroupedServerRestProxy.of(
            WorkerRpcService.class, workerRpcProvider, localGroupMatcher, discoverWorker, restTemplate, retry
        );

        this.destinationProxy = DestinationServerRestProxy.of(
            WorkerRpcService.class, workerRpcProvider, localWorker, restTemplate, RetryProperties.none()
        );
    }

    public List<Worker> getAliveWorkers(String group) {
        return discoverWorker.getAliveServers(group);
    }

    public boolean hasAliveWorker(String group) {
        return CollectionUtils.isNotEmpty(getAliveWorkers(group));
    }

    public boolean hasAliveWorker() {
        return discoverWorker.hasAliveServer();
    }

    public boolean hasAliveTask(List<SchedTask> tasks) {
        return CollectionUtils.isNotEmpty(tasks) &&
            tasks.stream().anyMatch(e -> e.isExecuting() && isAliveWorker(e.worker()));
    }

    public boolean shouldRedispatch(SchedTask task) {
        if (!task.isWaiting()) {
            return false;
        }

        Worker worker = task.worker();
        if (!isAliveWorker(worker)) {
            return true;
        }

        ExistsTaskParam param = ExistsTaskParam.of(worker.getGroup(), task.getTaskId());
        try {
            // `WorkerRpcService#existsTask`：判断任务是否在线程池中，如果不在则可能是没有分发成功，需要重新分发
            return !destination(worker).existsTask(param);
        } catch (Throwable e) {
            LOG.error("Invoke worker exists task error: " + worker, e);
            // 若调用异常(如请求超时)，则默认为Worker已存在该task，本次不做处理，等下一次扫描时再判断是否要重新分发任务
            return false;
        }
    }

    public WorkerRpcService destination(Worker destinationWorker) {
        return destinationProxy.destination(destinationWorker);
    }

    // --------------------------------------------------------------default package methods

    boolean isAliveWorker(Worker worker) {
        return worker != null && discoverWorker.isAliveServer(worker);
    }

    void verifyJob(SchedJob job) throws JobException {
        Assert.hasText(job.getJobExecutor(), "Job executor cannot be blank.");
        CoreUtils.checkClobMaximumLength(job.getJobExecutor(), "Job executor");
        CoreUtils.checkClobMaximumLength(job.getJobParam(), "Job param");
        JobType.of(job.getJobType());
        RouteStrategy.of(job.getRouteStrategy());

        VerifyJobParam param = ModelConverter.toVerifyJobParam(job);
        groupedProxy.group(job.getGroup()).verifyJob(param);
    }

    List<SchedTask> splitJob(String group, long instanceId, SplitJobParam param,
                             IdGenerator idGenerator, int maximumSplitTaskSize) throws JobException {
        List<Worker> workers = getAliveWorkers(group);
        Assert.notEmpty(workers, () -> "None alive worker for split job: " + group);
        int wCount = workers.size();

        param.setWorkerCount(wCount);
        SplitJobResult result = groupedProxy.group(group).splitJob(param);
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
            tasks.add(SchedTask.of(taskParams.get(i), idGenerator.generateId(), instanceId, i + 1, tCount, worker));
        }
        return tasks;
    }

    void dispatch(List<ExecuteTaskParam> tasks) {
        TransactionUtils.assertWithoutTransaction();
        taskDispatcher.dispatch(tasks);
    }

    void dispatch(String group, List<ExecuteTaskParam> tasks) {
        TransactionUtils.assertWithoutTransaction();
        taskDispatcher.dispatch(group, tasks);
    }

}
