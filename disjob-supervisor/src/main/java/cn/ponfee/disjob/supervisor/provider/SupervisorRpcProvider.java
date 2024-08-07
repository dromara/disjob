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

package cn.ponfee.disjob.supervisor.provider;

import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.spring.RpcController;
import cn.ponfee.disjob.core.base.*;
import cn.ponfee.disjob.core.dto.supervisor.EventParam;
import cn.ponfee.disjob.core.dto.supervisor.StartTaskParam;
import cn.ponfee.disjob.core.dto.supervisor.StartTaskResult;
import cn.ponfee.disjob.core.dto.supervisor.StopTaskParam;
import cn.ponfee.disjob.core.enums.Operation;
import cn.ponfee.disjob.supervisor.application.EventSubscribeService;
import cn.ponfee.disjob.supervisor.auth.SupervisorAuthentication;
import cn.ponfee.disjob.supervisor.component.DistributedJobManager;

import java.util.List;

/**
 * Supervisor rpc provider.
 *
 * @author Ponfee
 */
@RpcController
@SupervisorAuthentication(SupervisorAuthentication.Subject.WORKER)
public class SupervisorRpcProvider implements SupervisorRpcService {

    private final DistributedJobManager jobManager;

    public SupervisorRpcProvider(DistributedJobManager jobManager) {
        this.jobManager = jobManager;
    }

    @Override
    public StartTaskResult startTask(StartTaskParam param) {
        return jobManager.startTask(param);
    }

    @Override
    public void updateTaskWorker(String worker, List<Long> taskIds) {
        jobManager.updateTaskWorker(worker, taskIds);
    }

    @Override
    public boolean stopTask(StopTaskParam param) {
        return jobManager.stopTask(param);
    }

    @Override
    public boolean pauseInstance(long instanceId) {
        return jobManager.pauseInstance(instanceId);
    }

    @Override
    public boolean cancelInstance(long instanceId, Operation operation) {
        return jobManager.cancelInstance(instanceId, operation);
    }

    @SupervisorAuthentication(SupervisorAuthentication.Subject.ANON)
    @Override
    public SupervisorMetrics getMetrics() {
        SupervisorMetrics metrics = new SupervisorMetrics();
        metrics.setVersion(JobConstants.VERSION);
        metrics.setStartupAt(Dates.toDate(Supervisor.current().getStartupAt()));
        metrics.setAlsoWorker(Worker.current() != null);
        return metrics;
    }

    @SupervisorAuthentication(SupervisorAuthentication.Subject.ANON)
    @Override
    public void publishEvent(EventParam param) {
        EventSubscribeService.subscribe(param);
    }

    @Override
    public boolean savepoint(long taskId, String worker, String executeSnapshot) {
        return jobManager.savepoint(taskId, worker, executeSnapshot);
    }

}
