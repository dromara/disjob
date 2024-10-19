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

package cn.ponfee.disjob.worker.provider;

import cn.ponfee.disjob.common.spring.RpcController;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.base.WorkerMetrics;
import cn.ponfee.disjob.core.base.WorkerRpcService;
import cn.ponfee.disjob.core.dto.worker.*;
import cn.ponfee.disjob.core.dto.worker.ConfigureWorkerParam.Action;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.registry.Registry;
import cn.ponfee.disjob.worker.base.WorkerConfigurator;
import cn.ponfee.disjob.worker.executor.JobExecutorUtils;

import java.util.List;

/**
 * Worker rpc service provider.
 *
 * @author Ponfee
 */
@RpcController
public class WorkerRpcProvider implements WorkerRpcService {

    private final Worker.Local localWorker;
    private final Registry<Worker> workerRegistry;

    public WorkerRpcProvider(Worker.Local localWorker, Registry<Worker> workerRegistry) {
        this.localWorker = localWorker;
        this.workerRegistry = workerRegistry;
    }

    @Override
    public void verifyJob(VerifyJobParam param) throws JobException {
        localWorker.verifySupervisorAuthenticationToken(param);
        JobExecutorUtils.verify(param);
    }

    @Override
    public SplitJobResult splitJob(SplitJobParam param) throws JobException {
        localWorker.verifySupervisorAuthenticationToken(param);
        List<String> taskParams = JobExecutorUtils.split(param);
        return SplitJobResult.of(taskParams);
    }

    @Override
    public boolean existsTask(ExistsTaskParam param) {
        localWorker.verifySupervisorAuthenticationToken(param);
        return WorkerConfigurator.existsTask(param.getTaskId());
    }

    @Override
    public WorkerMetrics getMetrics(GetMetricsParam param) {
        String wGroup = localWorker.getGroup();
        String pGroup = param.getGroup();
        if (!wGroup.equals(pGroup)) {
            throw new IllegalArgumentException("Inconsistent get metrics group: " + wGroup + " != " + pGroup);
        }
        localWorker.verifySupervisorAuthenticationToken(param);

        return WorkerConfigurator.metrics();
    }

    @Override
    public void configureWorker(ConfigureWorkerParam param) {
        localWorker.verifySupervisorAuthenticationToken(param);

        Action action = param.getAction();
        if (action == Action.MODIFY_MAXIMUM_POOL_SIZE) {
            Integer maximumPoolSize = action.parse(param.getData());
            WorkerConfigurator.modifyMaximumPoolSize(maximumPoolSize);

        } else if (action == Action.REMOVE_WORKER) {
            workerRegistry.deregister(localWorker);

        } else if (action == Action.ADD_WORKER) {
            String cGroup = localWorker.getGroup();
            String dGroup = action.parse(param.getData());
            if (!cGroup.equals(dGroup)) {
                throw new UnsupportedOperationException("Inconsistent add worker group: " + cGroup + "!=" + dGroup);
            }
            workerRegistry.register(localWorker);

        } else {
            throw new UnsupportedOperationException("Unsupported configure worker action: " + action);
        }
    }

}
