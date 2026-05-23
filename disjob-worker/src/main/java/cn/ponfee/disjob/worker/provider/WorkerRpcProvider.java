/*
 * Copyright 2022-2026 Ponfee (http://www.ponfee.cn/)
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

import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.common.spring.RpcController;
import cn.ponfee.disjob.common.util.ProxyUtils;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.worker.Worker;
import cn.ponfee.disjob.core.worker.WorkerMetrics;
import cn.ponfee.disjob.core.worker.WorkerRpcService;
import cn.ponfee.disjob.core.worker.dto.*;
import cn.ponfee.disjob.core.worker.dto.ConfigureWorkerParam.Action;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.worker.base.WorkerConfigurator;
import cn.ponfee.disjob.worker.util.JobExecutorUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Worker rpc service provider.
 *
 * @author Ponfee
 */
@RpcController
public interface WorkerRpcProvider extends WorkerRpcService {

    /**
     * Creates WorkerRpcService proxy
     *
     * @param localWorker    the local worker
     * @param timingWheel    the timing wheel
     * @param workerRegistry the worker registry
     * @return WorkerRpcService proxy
     */
    static WorkerRpcProvider create(Worker.Local localWorker, TimingWheel<ExecuteTaskParam> timingWheel, WorkerRegistry workerRegistry) {
        return ProxyUtils.create(new WorkerRpcLocal(localWorker, timingWheel, workerRegistry), WorkerRpcProvider.class);
    }

    @Slf4j
    class WorkerRpcLocal implements InvocationHandler, WorkerRpcService {

        private final Worker.Local localWorker;
        private final TimingWheel<ExecuteTaskParam> timingWheel;
        private final WorkerRegistry workerRegistry;

        private WorkerRpcLocal(Worker.Local localWorker, TimingWheel<ExecuteTaskParam> timingWheel, WorkerRegistry workerRegistry) {
            this.localWorker = localWorker;
            this.timingWheel = timingWheel;
            this.workerRegistry = workerRegistry;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                // 要通过参数类型判断，防止参数值传null绕过认证
                if (AuthenticationParam.class.isAssignableFrom(parameterTypes[i])) {
                    AuthenticationParam param = (AuthenticationParam) args[i];
                    if (param != null) {
                        param.check();
                    }
                    localWorker.verifySupervisorAuthenticationToken(param);
                }
            }
            return method.invoke(this, args);
        }

        @Override
        public void subscribeSupervisorEvent(SupervisorEventParam param) {
            workerRegistry.subscribeServerEvent(param.getEventType(), param.getSupervisor());
        }

        @Override
        public void verifyJob(VerifyJobParam param) throws JobException {
            JobExecutorUtils.verify(param);
        }

        @Override
        public SplitJobResult splitJob(SplitJobParam param) throws JobException {
            return SplitJobResult.of(JobExecutorUtils.split(param));
        }

        @Override
        public boolean receiveTask(ExecuteTaskParam param) {
            if (param == null) {
                log.error("Received task param cannot be null.");
                return false;
            }

            localWorker.verifySupervisorAuthenticationToken(param);
            Worker assignedWorker = param.getWorker();
            if (!localWorker.matches(assignedWorker)) {
                log.error("Received unmatched worker task: {}, {}, {}", param.getTaskId(), localWorker, assignedWorker);
                return false;
            }

            if (!localWorker.getWorkerId().equals(assignedWorker.getWorkerId())) {
                // 当Worker宕机后又快速启动(重启)的情况，Supervisor从本地缓存(或注册中心)拿到的仍是旧的workerId，但任务却派发给新的workerId(同机器同端口)
                // 这种情况：1、可以剔除掉，等待Supervisor重新派发即可；2、也可以不剔除掉，短暂时间内该Worker的压力会是正常情况的2倍(注册中心还存有旧workerId)；
                log.warn("Received former worker task: {}, {}, {}", param.getTaskId(), localWorker, assignedWorker);
                param.setWorker(localWorker);
            }

            boolean res = timingWheel.offer(param);
            if (res) {
                log.info("Task trace [{}] received: {}, {}", param.getTaskId(), param.getOperation(), param.getWorker());
            } else {
                log.error("Received task failed: {}", param);
            }
            return res;
        }

        @Override
        public boolean existsTask(ExistsTaskParam param) {
            return WorkerConfigurator.existsTask(param.getTaskId());
        }

        @Override
        public WorkerMetrics getMetrics(GetMetricsParam param) {
            return WorkerConfigurator.metrics();
        }

        @Override
        public void configureWorker(ConfigureWorkerParam param) {
            Action action = param.getAction();
            switch (action) {
                case MODIFY_MAXIMUM_POOL_SIZE:
                    Integer maximumPoolSize = action.parse(param.getData());
                    WorkerConfigurator.modifyMaximumPoolSize(maximumPoolSize);
                    break;
                case REMOVE_WORKER:
                    workerRegistry.deregister(localWorker);
                    break;
                case ADD_WORKER:
                    workerRegistry.register(localWorker);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported configure worker action: " + action);
            }
        }
    }

}
