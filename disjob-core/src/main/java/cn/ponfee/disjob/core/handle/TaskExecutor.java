/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.handle;

import cn.ponfee.disjob.core.handle.execution.ExecutingTask;

/**
 * Task executor
 *
 * @author Ponfee
 */
public abstract class TaskExecutor {

    private volatile boolean stopped = false;

    /**
     * Stop execution.
     */
    public final void stop() {
        this.stopped = true;
        onStop();
    }

    /**
     * On stop
     */
    protected void onStop() {
        // default noop
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
     * Initializes task
     *
     * @param executingTask the executing task
     * @throws Exception if init failed
     */
    public void init(ExecutingTask executingTask) throws Exception { }

    /**
     * Executes task
     *
     * @param executingTask the executing task
     * @param savepoint     the savepoint
     * @return execute result
     * @throws Exception if execute failed
     */
    public abstract ExecuteResult execute(ExecutingTask executingTask, Savepoint savepoint) throws Exception;

    /**
     * Destroy this task executor
     */
    public void destroy() { }

}
