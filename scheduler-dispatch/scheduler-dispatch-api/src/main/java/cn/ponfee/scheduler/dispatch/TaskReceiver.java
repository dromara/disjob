package cn.ponfee.scheduler.dispatch;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import org.springframework.util.Assert;

/**
 * Worker receive the dispatched tasks
 *
 * @author Ponfee
 */
public abstract class TaskReceiver implements AutoCloseable {

    private final TimingWheel<ExecuteParam> timingWheel;

    protected TaskReceiver(TimingWheel<ExecuteParam> timingWheel) {
        Assert.notNull(timingWheel, "Timing wheel cannot null.");
        this.timingWheel = timingWheel;
    }

    /**
     * Receives the supervisor dispatched tasks,
     * after received then do call post-receive
     *
     * @see #postReceive(ExecuteParam)
     */
    protected abstract void receive();

    protected final void postReceive(ExecuteParam executeParam) {
        if (executeParam != null) {
            timingWheel.offer(executeParam);
        }
    }

    /**
     * Start do receive
     */
    public abstract void start();

    /**
     * Close resources if necessary.
     */
    @Override
    public void close() {
        // No-op
    }

    public TimingWheel<ExecuteParam> getTimingWheel() {
        return timingWheel;
    }

}
