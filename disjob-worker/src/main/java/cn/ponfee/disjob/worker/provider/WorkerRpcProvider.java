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
import cn.ponfee.disjob.common.util.ProxyUtils;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.base.WorkerMetrics;
import cn.ponfee.disjob.core.base.WorkerRpcService;
import cn.ponfee.disjob.core.dto.worker.*;
import cn.ponfee.disjob.core.dto.worker.ConfigureWorkerParam.Action;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.worker.base.WorkerConfigurator;
import cn.ponfee.disjob.worker.executor.JobExecutorUtils;

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
     * @param localWorker    the localWorker
     * @param workerRegistry the workerRegistry
     * @return WorkerRpcService proxy
     */
    static WorkerRpcProvider create(Worker.Local localWorker, WorkerRegistry workerRegistry) {
        InvocationHandler invocationHandler = new AuthenticateHandler(localWorker, workerRegistry);
        return ProxyUtils.create(invocationHandler, WorkerRpcProvider.class);
    }

    class AuthenticateHandler implements InvocationHandler {
        private final Worker.Local localWorker;
        private final WorkerRpcService target;

        private AuthenticateHandler(Worker.Local localWorker, WorkerRegistry workerRegistry) {
            this.localWorker = localWorker;
            this.target = new WorkerRpcImpl(localWorker, workerRegistry);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                // 要通过参数类型判断，防止参数值传null绕过认证
                if (AuthenticationParam.class.isAssignableFrom(parameterTypes[i])) {
                    localWorker.verifySupervisorAuthenticationToken((AuthenticationParam) args[i]);
                }
            }
            return method.invoke(target, args);
        }
    }

    class WorkerRpcImpl implements WorkerRpcService {
        private final Worker.Local localWorker;
        private final WorkerRegistry workerRegistry;

        private WorkerRpcImpl(Worker.Local localWorker, WorkerRegistry workerRegistry) {
            this.localWorker = localWorker;
            this.workerRegistry = workerRegistry;
        }

        @Override
        public void subscribeSupervisorChanged(SubscribeSupervisorChangedParam param) {
            param.check();
            workerRegistry.subscribeServerChanged(param.getEventType(), param.getSupervisor());
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
