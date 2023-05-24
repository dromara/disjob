/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.handle;

import cn.ponfee.disjob.common.model.Result;
import cn.ponfee.disjob.core.model.SchedTask;

/**
 * Schedule task executor
 *
 * @author Ponfee
 */
public abstract class TaskExecutor<T> {

    private volatile boolean stopped = false;

    private SchedTask task;

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
     * Stop execute.
     */
    public final void stop() {
        this.stopped = true;
    }

    /**
     * Returns execute is whether stopped.
     *
     * @return {@code true} if stopped
     */
    public final boolean isStopped() {
        return stopped;
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
