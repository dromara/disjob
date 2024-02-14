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

package cn.ponfee.disjob.worker.base;

import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.base.WorkerMetrics;

/**
 * Worker configurator
 *
 * @author Ponfee
 */
public class WorkerConfigurator {

    private static volatile WorkerThreadPool workerThreadPool;

    static synchronized void setWorkerThreadPool(WorkerThreadPool threadPool) {
        if (workerThreadPool != null) {
            throw new AssertionError("WorkerThreadPool already set.");
        }
        workerThreadPool = threadPool;
    }

    public static WorkerMetrics metrics() {
        Worker.Current currentWorker = Worker.current();
        WorkerMetrics metrics = new WorkerMetrics();
        metrics.setVersion(JobConstants.VERSION);
        metrics.setWorkerId(currentWorker.getWorkerId());
        metrics.setStartupAt(Dates.toDate(currentWorker.getStartupAt()));
        metrics.setAlsoSupervisor(Supervisor.current() != null);
        metrics.setJvmThreadActiveCount(Thread.activeCount());
        if (workerThreadPool != null) {
            metrics.setThreadPool(workerThreadPool.metrics());
        }
        metrics.setSignature(currentWorker.createWorkerSignatureToken());
        return metrics;
    }

    public static void modifyMaximumPoolSize(int maximumPoolSize) {
        workerThreadPool.modifyMaximumPoolSize(maximumPoolSize);
    }

    public static void clearTaskQueue() {
        workerThreadPool.clearTaskQueue();
    }

}
