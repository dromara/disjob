/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.worker.handle;

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
     * @param task the execution task
     * @throws Exception if init failed
     */
    public void init(ExecuteTask task) throws Exception { }

    /**
     * Executes task
     *
     * @param task      the execution task
     * @param savepoint the savepoint
     * @return execute result
     * @throws Exception if execute failed
     */
    public abstract ExecuteResult execute(ExecuteTask task, Savepoint savepoint) throws Exception;

    /**
     * Destroy this task executor
     */
    public void destroy() { }

}
