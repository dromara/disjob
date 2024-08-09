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

package cn.ponfee.disjob.dispatch;

import cn.ponfee.disjob.common.base.Startable;
import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.core.base.Worker;
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

    private final Worker.Current currentWorker;
    private final TimingWheel<ExecuteTaskParam> timingWheel;

    protected TaskReceiver(Worker.Current currentWorker, TimingWheel<ExecuteTaskParam> timingWheel) {
        this.timingWheel = Objects.requireNonNull(timingWheel, "Timing wheel cannot be null.");
        this.currentWorker = Objects.requireNonNull(currentWorker, "Current worker cannot be null.");
    }

    /**
     * Receive task
     *
     * @param task the task param
     * @return {@code true} if received successful
     */
    public abstract boolean receive(ExecuteTaskParam task);

    /**
     * Receives the supervisor dispatched tasks.
     *
     * @param param the task param
     * @return {@code true} if received task successfully
     */
    protected final boolean doReceive(ExecuteTaskParam param) {
        if (param == null) {
            log.error("Received task cannot be null.");
            return false;
        }

        currentWorker.verifySupervisorAuthenticationToken(param);

        Worker assignedWorker = param.getWorker();
        if (!currentWorker.matches(assignedWorker)) {
            log.error("Received unmatched worker task: {}, {}, {}", param.getTaskId(), currentWorker, assignedWorker);
            return false;
        }
        if (!currentWorker.getWorkerId().equals(assignedWorker.getWorkerId())) {
            // 当Worker宕机后又快速启动(重启)的情况，Supervisor从本地缓存(或注册中心)拿到的仍是旧的workerId，但任务却Http方式派发给新的workerId(同机器同端口)
            // 这种情况：1、可以剔除掉，等待Supervisor重新派发即可；2、也可以不剔除掉，短暂时间内该Worker的压力会是正常情况的2倍(注册中心还存有旧workerId)；
            log.warn("Received former worker task: {}, {}, {}", param.getTaskId(), currentWorker, assignedWorker);
            param.setWorker(currentWorker);
        }

        boolean res = timingWheel.offer(param);
        if (res) {
            log.info("Task trace [{}] received: {}, {}", param.getTaskId(), param.getOperation(), param.getWorker());
        } else {
            log.error("Received task failed {}", param);
        }
        return res;
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
