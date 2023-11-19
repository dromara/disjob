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
    private static final Logger LOG = LoggerFactory.getLogger(TaskReceiver.class);

    private final Worker currentWorker;
    private final TimingWheel<ExecuteTaskParam> timingWheel;

    public TaskReceiver(Worker currentWorker, TimingWheel<ExecuteTaskParam> timingWheel) {
        this.timingWheel = Objects.requireNonNull(timingWheel, "Timing wheel cannot be null.");
        this.currentWorker = Objects.requireNonNull(currentWorker, "Current worker cannot be null.");
    }

    /**
     * Receives the supervisor dispatched tasks.
     *
     * @param param the execution task param
     */
    public boolean receive(ExecuteTaskParam param) {
        if (param == null) {
            LOG.error("Received task cannot be null.");
            return false;
        }

        Worker assignedWorker = param.getWorker();
        if (!currentWorker.sameWorker(assignedWorker)) {
            LOG.error("Received unmatched worker: {} | '{}' | '{}'", param.getTaskId(), currentWorker, assignedWorker);
            return false;
        }
        if (!currentWorker.getWorkerId().equals(assignedWorker.getWorkerId())) {
            // 当Worker宕机后又快速启动(重启)的情况，Supervisor从本地缓存(或注册中心)拿到的仍是旧的workerId，但任务却Http方式派发给新的workerId(同机器同端口)
            // 这种情况：1、可以剔除掉，等待Supervisor重新派发即可；2、也可以不剔除掉，短暂时间内该Worker的压力会是正常情况的2倍(注册中心还存有旧workerId)；
            LOG.warn("Received former worker: {} | '{}' | '{}'", param.getTaskId(), currentWorker, assignedWorker);
        }

        boolean res = timingWheel.offer(param);
        if (res) {
            LOG.info("Task trace [received]: {} | {} | {}", param.getTaskId(), param.getOperation(), param.getWorker());
        } else {
            LOG.error("Received task failed " + param);
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
