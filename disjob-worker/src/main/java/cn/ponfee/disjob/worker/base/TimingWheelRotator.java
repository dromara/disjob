/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.worker.base;

import cn.ponfee.disjob.common.base.LoopProcessThread;
import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.base.Startable;
import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.common.concurrent.NamedThreadFactory;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingSupplier;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.SupervisorCoreRpcService;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.param.ExecuteTaskParam;
import cn.ponfee.disjob.core.param.TaskWorkerParam;
import cn.ponfee.disjob.registry.Discovery;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import static cn.ponfee.disjob.core.base.JobConstants.PROCESS_BATCH_SIZE;

/**
 * The thread for rotating timing wheel.
 *
 * @author Ponfee
 */
public class TimingWheelRotator extends SingletonClassConstraint implements Startable {

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance(Dates.DATEFULL_PATTERN);
    private static final Logger LOG = LoggerFactory.getLogger(TimingWheelRotator.class);

    private final Worker currentWorker;
    private final SupervisorCoreRpcService supervisorCoreRpcClient;
    private final Discovery<Supervisor> discoverySupervisor;
    private final TimingWheel<ExecuteTaskParam> timingWheel;
    private final WorkerThreadPool workerThreadPool;
    private final LoopProcessThread heartbeatThread;
    private final ExecutorService processExecutor;

    private volatile int round = 0;

    public TimingWheelRotator(Worker currentWorker,
                              SupervisorCoreRpcService supervisorCoreRpcClient,
                              Discovery<Supervisor> discoverySupervisor,
                              TimingWheel<ExecuteTaskParam> timingWheel,
                              WorkerThreadPool threadPool,
                              int processThreadPoolSize) {
        this.currentWorker = currentWorker;
        this.supervisorCoreRpcClient = supervisorCoreRpcClient;
        this.discoverySupervisor = discoverySupervisor;
        this.timingWheel = timingWheel;
        this.workerThreadPool = threadPool;

        this.heartbeatThread = new LoopProcessThread("timing_wheel_rotate", timingWheel.getTickMs(), 0, this::process);

        int actualProcessPoolSize = Math.max(1, processThreadPoolSize);
        this.processExecutor = ThreadPoolExecutors.builder()
            .corePoolSize(actualProcessPoolSize)
            .maximumPoolSize(actualProcessPoolSize)
            .workQueue(new LinkedBlockingQueue<>(Integer.MAX_VALUE))
            .keepAliveTimeSeconds(300)
            .threadFactory(NamedThreadFactory.builder().prefix("timing_wheel_process").build())
            .build();
    }

    @Override
    public void start() {
        heartbeatThread.start();
    }

    @Override
    public void stop() {
        if (heartbeatThread.terminate()) {
            ThrowingSupplier.execute(() -> ThreadPoolExecutors.shutdown(processExecutor, 3));
        }
    }

    private void process() {
        if (++round > 1024) {
            round = 0;
            LOG.info("Timing wheel rotator heartbeat: worker-thread-pool={}, jvm-thread-count={}", workerThreadPool, Thread.activeCount());
        }

        // check has available supervisors
        if (!discoverySupervisor.hasDiscoveredServers()) {
            if ((round & 0x1F) == 0) {
                LOG.warn("Not found available supervisor.");
            }
            return;
        }

        final List<ExecuteTaskParam> tasks = timingWheel.poll();
        if (!tasks.isEmpty()) {
            processExecutor.execute(() -> process(tasks));
        }
    }

    private void process(List<ExecuteTaskParam> tasks) {
        List<ExecuteTaskParam> matchedTasks = tasks.stream()
            .filter(e -> {
                Worker assignedWorker = e.getWorker();
                if (!currentWorker.sameWorker(assignedWorker)) {
                    LOG.error("Processed unmatched worker: {} | '{}' | '{}'", e.getTaskId(), currentWorker, assignedWorker);
                    return false;
                }
                if (!currentWorker.getWorkerId().equals(assignedWorker.getWorkerId())) {
                    // 当Worker宕机后又快速启动(重启)的情况，Supervisor从本地缓存(或注册中心)拿到的仍是旧的workerId，但任务却Http方式派发给新的workerId(同机器同端口)
                    // 这种情况：1、可以剔除掉，等待Supervisor重新派发即可；2、也可以不剔除掉，短暂时间内该Worker的压力会是正常情况的2倍(注册中心还存有旧workerId)；
                    LOG.warn("Processed former worker: {} | '{}' | '{}'", e.getTaskId(), currentWorker, assignedWorker);
                }
                LOG.info("Processed task {} | {} | {} | {}", e.getTaskId(), e.getOperation(), assignedWorker, DATE_FORMAT.format(e.getTriggerTime()));
                return true;
            })
            .collect(Collectors.toList());

        if (matchedTasks.isEmpty()) {
            return;
        }

        List<List<ExecuteTaskParam>> partitions = Lists.partition(matchedTasks, PROCESS_BATCH_SIZE);
        for (List<ExecuteTaskParam> batchTasks : partitions) {
            List<TaskWorkerParam> list = batchTasks.stream()
                .filter(e -> e.getRouteStrategy() != RouteStrategy.BROADCAST)
                .map(e -> new TaskWorkerParam(e.getTaskId(), e.getWorker().serialize()))
                .collect(Collectors.toList());
            ThrowingRunnable.execute(() -> supervisorCoreRpcClient.updateTaskWorker(list), () -> "Update task worker error: " + Jsons.toJson(list));
            batchTasks.forEach(workerThreadPool::submit);
        }
    }

}
