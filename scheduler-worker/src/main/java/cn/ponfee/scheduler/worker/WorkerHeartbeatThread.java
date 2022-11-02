package cn.ponfee.scheduler.worker;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.core.base.AbstractHeartbeatThread;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.registry.Discovery;
import cn.ponfee.scheduler.worker.base.WorkerThreadPool;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * Worker process task heartbeat.
 *
 * @author Ponfee
 */
public class WorkerHeartbeatThread extends AbstractHeartbeatThread {

    private final Discovery<Supervisor> discoverySupervisor;
    private final TimingWheel<ExecuteParam> timingWheel;
    private final WorkerThreadPool workerThreadPool;

    private int round = 0;

    public WorkerHeartbeatThread(Discovery<Supervisor> discoverySupervisor,
                                 TimingWheel<ExecuteParam> timingWheel,
                                 WorkerThreadPool threadPool) {
        super(1);
        this.discoverySupervisor = discoverySupervisor;
        this.timingWheel = timingWheel;
        this.workerThreadPool = threadPool;
    }

    @Override
    protected boolean heartbeat() {
        if (++round == 120) {
            round = 0;
            logger.info("worker-thread-pool: {}, jvm-active-count: {}", workerThreadPool, Thread.activeCount());
        }

        return process();
    }

    private boolean process() {
        // check has available supervisors
        if (CollectionUtils.isEmpty(discoverySupervisor.getServers())) {
            logger.warn("Not available supervisors.");
            return false;
        }

        List<ExecuteParam> ringTrigger = timingWheel.poll();
        if (ringTrigger.isEmpty()) {
            return false;
        }

        for (ExecuteParam param : ringTrigger) {
            workerThreadPool.submit(param);
        }
        ringTrigger.clear();

        return true;
    }

}
