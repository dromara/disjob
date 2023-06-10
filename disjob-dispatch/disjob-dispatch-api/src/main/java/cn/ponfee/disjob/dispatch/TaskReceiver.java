/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.dispatch;

import cn.ponfee.disjob.common.base.Startable;
import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.core.param.ExecuteTaskParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Worker receive dispatched task from supervisor.
 *
 * @author Ponfee
 */
public abstract class TaskReceiver implements Startable {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final TimingWheel<ExecuteTaskParam> timingWheel;

    public TaskReceiver(TimingWheel<ExecuteTaskParam> timingWheel) {
        this.timingWheel = Objects.requireNonNull(timingWheel, "Timing wheel cannot be null.");
    }

    /**
     * Receives the supervisor dispatched tasks.
     *
     * @param param the execution task param
     */
    public boolean receive(ExecuteTaskParam param) {
        boolean result = param != null && timingWheel.offer(param);
        if (result) {
            log.info("Received task {} | {} | {}", param.getTaskId(), param.getOperation(), param.getWorker());
        } else {
            log.error("Received task filed {}", param);
        }
        return result;
    }

    /**
     * Start do receive
     */
    @Override
    public void start() {
        // No-op
    }

    /**
     * Close resources if necessary.
     */
    @Override
    public void stop() {
        // No-op
    }

    public final TimingWheel<ExecuteTaskParam> getTimingWheel() {
        return timingWheel;
    }

}
