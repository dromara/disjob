/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.worker.base;

import cn.ponfee.disjob.common.base.Startable;
import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.common.concurrent.NamedThreadFactory;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingSupplier;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.SupervisorService;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static cn.ponfee.disjob.core.base.JobConstants.PROCESS_BATCH_SIZE;

/**
 * The thread for rotating timing wheel.
 *
 * @author Ponfee
 */
public class RotatingTimingWheel implements Startable {

    private static final Logger LOG = LoggerFactory.getLogger(RotatingTimingWheel.class);

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance(Dates.FULL_DATE_FORMAT);

    private static final int LOG_ROUND_COUNT = 1000;

    private final Worker currentWorker;
    private final SupervisorService supervisorServiceClient;
    private final Discovery<Supervisor> discoverySupervisor;
    private final TimingWheel<ExecuteTaskParam> timingWheel;
    private final WorkerThreadPool workerThreadPool;

    private final ScheduledExecutorService scheduledExecutor;
    private final ExecutorService updateTaskWorkerExecutor;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private int round = 0;

    public RotatingTimingWheel(Worker currentWorker,
                               SupervisorService supervisorServiceClient,
                               Discovery<Supervisor> discoverySupervisor,
                               TimingWheel<ExecuteTaskParam> timingWheel,
                               WorkerThreadPool threadPool,
                               int updateTaskWorkerThreadPoolSize) {
        this.currentWorker = currentWorker;
        this.supervisorServiceClient = supervisorServiceClient;
        this.discoverySupervisor = discoverySupervisor;
        this.timingWheel = timingWheel;
        this.workerThreadPool = threadPool;

        this.scheduledExecutor = new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = new Thread(r, "rotating_timing_wheel");
            thread.setDaemon(true);
            thread.setPriority(Thread.MAX_PRIORITY);
            return thread;
        });

        int poolSize = Math.max(1, updateTaskWorkerThreadPoolSize);
        this.updateTaskWorkerExecutor = ThreadPoolExecutors.builder()
            .corePoolSize(poolSize)
            .maximumPoolSize(poolSize)
            .workQueue(new LinkedBlockingQueue<>(Integer.MAX_VALUE))
            .keepAliveTimeSeconds(300)
            .threadFactory(NamedThreadFactory.builder().prefix("update_task_worker").build())
            .prestartCoreThreadType(ThreadPoolExecutors.PrestartCoreThreadType.ONE)
            .build();
    }

    @Override
    public void start() {
        if (!started.compareAndSet(false, true)) {
            LOG.warn("Repeat do start rotating timing wheel.");
            return;
        }
        scheduledExecutor.scheduleAtFixedRate(
            this::process,
            timingWheel.getTickMs(),
            timingWheel.getTickMs(),
            TimeUnit.MILLISECONDS
        );
    }

    private void process() {
        if (++round == LOG_ROUND_COUNT) {
            round = 0;
            LOG.info("worker-thread-pool: {}, jvm-thread-count: {}", workerThreadPool, Thread.activeCount());
        }

        // check has available supervisors
        if (!discoverySupervisor.hasDiscoveredServers()) {
            if ((round & 0x1F) == 0) {
                LOG.warn("Not found available supervisor.");
            }
            return;
        }

        List<ExecuteTaskParam> ringTriggers = timingWheel.poll();
        if (ringTriggers.isEmpty()) {
            return;
        }

        List<ExecuteTaskParam> matchedTriggers = ringTriggers.stream()
            .filter(e -> {
                if (LOG.isInfoEnabled()) {
                    String triggerTime = DATE_FORMAT.format(e.getTriggerTime());
                    LOG.info("Took task {} | {} | {} | {}", e.getTaskId(), e.getOperation(), e.getWorker(), triggerTime);
                }
                if (currentWorker.equalsGroup(e.getWorker())) {
                    return true;
                } else {
                    LOG.error("The current worker '{}' cannot match expect worker '{}'", currentWorker, e.getWorker());
                    return false;
                }
            })
            .collect(Collectors.toList());
        if (matchedTriggers.isEmpty()) {
            return;
        }

        updateTaskWorkerExecutor.execute(() -> {
            List<List<ExecuteTaskParam>> partition = Lists.partition(matchedTriggers, PROCESS_BATCH_SIZE);
            for (List<ExecuteTaskParam> batchTriggers : partition) {
                List<TaskWorkerParam> list = batchTriggers.stream()
                    .filter(e -> e.getRouteStrategy() != RouteStrategy.BROADCAST)
                    .map(e -> new TaskWorkerParam(e.getTaskId(), e.getWorker().serialize()))
                    .collect(Collectors.toList());
                try {
                    supervisorServiceClient.updateTaskWorker(list);
                } catch (Throwable t) {
                    // must do submit if occur exception
                    LOG.error("Update task worker error: " + Jsons.toJson(list), t);
                }
                batchTriggers.forEach(workerThreadPool::submit);
            }
        });
    }

    @Override
    public void stop() {
        ThrowingSupplier.caught(() -> ThreadPoolExecutors.shutdown(scheduledExecutor, 3));
        ThrowingSupplier.caught(() -> ThreadPoolExecutors.shutdown(updateTaskWorkerExecutor, 3));
    }

}
