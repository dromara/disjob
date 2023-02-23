/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.worker.thread;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.base.AbstractHeartbeatThread;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.SupervisorService;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.registry.Discovery;
import cn.ponfee.scheduler.worker.base.WorkerThreadPool;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.stream.Collectors;

import static cn.ponfee.scheduler.core.base.JobConstants.PROCESS_BATCH_SIZE;

/**
 * The thread for rotating timing wheel.
 *
 * @author Ponfee
 */
public class RotatingTimingWheel extends AbstractHeartbeatThread {

    private static final int LOG_ROUND_COUNT = 1000;

    private final Worker currentWorker;
    private final SupervisorService supervisorServiceClient;
    private final Discovery<Supervisor> discoverySupervisor;
    private final TimingWheel<ExecuteParam> timingWheel;
    private final WorkerThreadPool workerThreadPool;

    private int round = 0;

    public RotatingTimingWheel(Worker currentWorker,
                               SupervisorService supervisorServiceClient,
                               Discovery<Supervisor> discoverySupervisor,
                               TimingWheel<ExecuteParam> timingWheel,
                               WorkerThreadPool threadPool) {
        super(timingWheel.getTickMs());
        this.currentWorker = currentWorker;
        this.supervisorServiceClient = supervisorServiceClient;
        this.discoverySupervisor = discoverySupervisor;
        this.timingWheel = timingWheel;
        this.workerThreadPool = threadPool;
    }

    @Override
    protected boolean heartbeat() {
        if (++round == LOG_ROUND_COUNT) {
            round = 0;
            log.info("worker-thread-pool: {}, jvm-active-count: {}", workerThreadPool, Thread.activeCount());
        }

        process();

        return true;
    }

    private void process() {
        // check has available supervisors
        if (!discoverySupervisor.hasDiscoveredServers()) {
            if ((round & 0x1F) == 0) {
                log.warn("Not found available supervisor.");
            }
            return;
        }

        List<ExecuteParam> ringTriggers = timingWheel.poll();
        if (ringTriggers.isEmpty()) {
            return;
        }

        List<ExecuteParam> matchedTriggers = ringTriggers.stream()
            .filter(e -> {
                if (currentWorker.equals(e.getWorker())) {
                    return true;
                } else {
                    log.error("The current worker '{}' cannot match expect worker '{}'", currentWorker, e.getWorker());
                    return false;
                }
            })
            .collect(Collectors.toList());
        if (matchedTriggers.isEmpty()) {
            return;
        }

        for (List<ExecuteParam> batchTriggers : Lists.partition(matchedTriggers, PROCESS_BATCH_SIZE)) {
            List<Long> batchTaskIds = batchTriggers.stream().map(ExecuteParam::getTaskId).collect(Collectors.toList());
            boolean status;
            try {
                status = supervisorServiceClient.updateTaskWorker(batchTaskIds, currentWorker.serialize());
            } catch (Exception e) {
                // must do submit if occur exception
                status = true;
                log.error("Update waiting sched_task.worker column failed: " + Jsons.toJson(batchTaskIds), e);
            }
            if (status) {
                batchTriggers.forEach(workerThreadPool::submit);
            }
        }
    }

}