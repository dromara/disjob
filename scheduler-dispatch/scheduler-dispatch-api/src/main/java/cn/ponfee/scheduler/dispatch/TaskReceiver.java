package cn.ponfee.scheduler.dispatch;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import org.springframework.util.Assert;

/**
 * Worker receive dispatched task from supervisor.
 *
 * @author Ponfee
 */
public abstract class TaskReceiver implements AutoCloseable {

    private final TimingWheel<ExecuteParam> timingWheel;

    public TaskReceiver(TimingWheel<ExecuteParam> timingWheel) {
        Assert.notNull(timingWheel, "Timing wheel cannot null.");
        this.timingWheel = timingWheel;
    }

    /**
     * Receives the supervisor dispatched tasks.
     *
     * @param executeParam the executeParam
     */
    public boolean receive(ExecuteParam executeParam) {
        return executeParam != null && timingWheel.offer(executeParam);
    }

    /**
     * Start do receive
     */
    public void start() {
        // No-op
    }

    /**
     * Close resources if necessary.
     */
    @Override
    public void close() {
        // No-op
    }

    public final TimingWheel<ExecuteParam> getTimingWheel() {
        return timingWheel;
    }

}
