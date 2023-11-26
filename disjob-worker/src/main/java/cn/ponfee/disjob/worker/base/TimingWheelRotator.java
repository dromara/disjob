/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.worker.base;

import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.base.Startable;
import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.common.concurrent.LoopThread;
import cn.ponfee.disjob.common.concurrent.NamedThreadFactory;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.SupervisorRpcService;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.param.supervisor.UpdateTaskWorkerParam;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
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

    private final SupervisorRpcService supervisorRpcClient;
    private final Discovery<Supervisor> discoverySupervisor;
    private final TimingWheel<ExecuteTaskParam> timingWheel;
    private final WorkerThreadPool workerThreadPool;
    private final LoopThread heartbeatThread;
    private final ExecutorService processExecutor;

    private volatile int round = 0;

    public TimingWheelRotator(SupervisorRpcService supervisorRpcClient,
                              Discovery<Supervisor> discoverySupervisor,
                              TimingWheel<ExecuteTaskParam> timingWheel,
                              WorkerThreadPool threadPool,
                              int processThreadPoolSize) {
        this.supervisorRpcClient = supervisorRpcClient;
        this.discoverySupervisor = discoverySupervisor;
        this.timingWheel = timingWheel;
        this.workerThreadPool = threadPool;

        this.heartbeatThread = new LoopThread("timing_wheel_rotate", timingWheel.getTickMs(), 0, this::process);

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
            ThreadPoolExecutors.shutdown(processExecutor, 2);
        }
    }

    private void process() {
        if (++round > 1024) {
            round = 0;
            LOG.info("Timing wheel rotator heartbeat, worker-thread-pool: {}, jvm-thread-count: {}", workerThreadPool, Thread.activeCount());
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
        for (List<ExecuteTaskParam> subs : Lists.partition(tasks, PROCESS_BATCH_SIZE)) {
            List<UpdateTaskWorkerParam> list = subs.stream()
                // 广播任务分派的worker不可修改，需要排除
                .filter(e -> e.getRouteStrategy() != RouteStrategy.BROADCAST)
                .map(e -> new UpdateTaskWorkerParam(e.getTaskId(), e.getWorker()))
                .collect(Collectors.toList());
            // 更新task的worker信息
            ThrowingRunnable.doCaught(() -> supervisorRpcClient.updateTaskWorker(list), () -> "Update task worker error: " + Jsons.toJson(list));

            // 触发执行
            subs.forEach(e -> {
                LOG.info("Task trace [triggered]: {} | {} | {} | {}", e.getTaskId(), e.getOperation(), e.getWorker(), DATE_FORMAT.format(e.getTriggerTime()));
                workerThreadPool.submit(e);
            });
        }
    }

}
