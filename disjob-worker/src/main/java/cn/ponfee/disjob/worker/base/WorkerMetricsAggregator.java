/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.worker.base;

import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.base.WorkerMetrics;

/**
 * Worker metrics aggregator
 *
 * @author Ponfee
 */
public class WorkerMetricsAggregator {

    private static volatile WorkerThreadPool workerThreadPool;

    static synchronized void setWorkerThreadPool(WorkerThreadPool threadPool) {
        if (workerThreadPool != null) {
            throw new AssertionError("WorkerThreadPool already set.");
        }
        workerThreadPool = threadPool;
    }

    public static WorkerMetrics metrics() {
        WorkerMetrics metrics = new WorkerMetrics();
        metrics.setStartupAt(Dates.toDate(Worker.current().getStartupAt()));
        metrics.setAlsoSupervisor(Supervisor.current() != null);
        metrics.setJvmThreadActiveCount(Thread.activeCount());
        if (workerThreadPool != null) {
            metrics.setThreadPool(workerThreadPool.metrics());
        }
        return metrics;
    }

    public static void modifyMaximumPoolSize(int maximumPoolSize) {
        workerThreadPool.modifyMaximumPoolSize(maximumPoolSize);
    }

}
