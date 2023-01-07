/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.handle;

import cn.ponfee.scheduler.common.base.model.Result;
import cn.ponfee.scheduler.core.model.SchedTask;

/**
 * Schedule task executor
 *
 * @author Ponfee
 */
public abstract class TaskExecutor<T> {

    private volatile boolean interrupted = false;

    protected SchedTask task;

    /**
     * Setting the sched task object.
     *
     * @param task the sched task object
     */
    public final void task(SchedTask task) {
        this.task = task;
    }

    /**
     * Returns the sched task object.
     *
     * @return sched task object
     */
    public final SchedTask task() {
        return task;
    }

    /**
     * Interrupts execute.
     */
    public final void interrupt() {
        this.interrupted = true;
    }

    /**
     * Returns execute is whether interrupt.
     *
     * @return {@code true} if interrupted
     */
    public final boolean isInterrupted() {
        return interrupted;
    }

    /**
     * Verifies schedule task
     *
     * @throws Exception if verified failed
     */
    public void verify() throws Exception { }

    /**
     * Initializes schedule task
     *
     * @throws Exception if init failed
     */
    public void init() throws Exception { }

    /**
     * Executes schedule task
     *
     * @param checkpoint the checkpoint
     * @return execution result
     * @throws Exception if execute failed
     */
    public abstract Result<T> execute(Checkpoint checkpoint) throws Exception;

    /**
     * Destroy this task executor
     */
    public void destroy() { }

}
