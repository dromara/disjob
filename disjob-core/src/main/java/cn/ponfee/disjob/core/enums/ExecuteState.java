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

package cn.ponfee.disjob.core.enums;

import cn.ponfee.disjob.common.base.IntValueEnum;
import cn.ponfee.disjob.common.util.Enums;

import java.util.Map;
import java.util.Objects;

/**
 * The task executed state enum definition.
 * <p>mapped by sched_task.execute_state
 *
 * @author Ponfee
 */
public enum ExecuteState implements IntValueEnum<ExecuteState> {

    /**
     * 等待执行
     */
    WAITING(10, RunState.WAITING, "等待执行"),

    /**
     * 正在执行
     */
    EXECUTING(20, RunState.RUNNING, "正在执行"),

    /**
     * 暂停执行
     */
    PAUSED(30, RunState.PAUSED, "暂停执行"),

    /**
     * 执行完成
     */
    COMPLETED(40, RunState.COMPLETED, "执行完成"),

    /**
     * 派发失败取消(任务派发失败的次数sched_task.dispatch_failed_count > SupervisorProperties#taskDispatchFailedCountThreshold)
     */
    DISPATCH_FAILED(50, RunState.CANCELED, "派发失败"),

    /**
     * 初始化异常取消
     */
    INITIALIZE_EXCEPTION(51, RunState.CANCELED, "初始化异常"),

    /**
     * 执行失败取消(`JobExecutor#execute`方法的返回结果为null或code!=0)
     */
    EXECUTE_FAILED(52, RunState.CANCELED, "执行失败"),

    /**
     * 执行异常取消
     */
    EXECUTE_EXCEPTION(53, RunState.CANCELED, "执行异常"),

    /**
     * 执行超时取消(任务的执行耗时 > sched_job.execute_timeout > 0)
     */
    EXECUTE_TIMEOUT(54, RunState.CANCELED, "执行超时"),

    /**
     * 执行冲突取消(sched_job.collided_strategy=3 并且 当前任务还未执行完成 时被取消)
     */
    EXECUTE_COLLIDED(55, RunState.CANCELED, "执行冲突"),

    /**
     * 广播任务终止(广播任务分派的Worker已下线导致未执行)
     */
    BROADCAST_ABORTED(56, RunState.CANCELED, "广播终止"),

    /**
     * 执行终止(如执行过程中Worker异常关机)
     */
    EXECUTE_ABORTED(57, RunState.CANCELED, "执行终止"),

    /**
     * Worker关机取消(sched_job.shutdown_strategy=3 并且 Worker正常关闭)
     */
    SHUTDOWN_CANCELED(58, RunState.CANCELED, "关机取消"),

    /**
     * 手动取消
     */
    MANUAL_CANCELED(59, RunState.CANCELED, "手动取消"),

    ;

    private final int value;

    /**
     * mapped sched_instance.run_state
     */
    private final RunState runState;

    private final String desc;

    ExecuteState(int value, RunState runState, String desc) {
        this.value = value;
        this.runState = runState;
        this.desc = desc;
    }

    @Override
    public int value() {
        return value;
    }

    @Override
    public String desc() {
        return desc;
    }

    public RunState runState() {
        return runState;
    }

    public boolean isPausable() {
        return this == WAITING || this == EXECUTING;
    }

    public boolean isTerminal() {
        return runState.isTerminal();
    }

    public boolean isFailure() {
        return runState.isFailure();
    }

    public static ExecuteState of(Integer value) {
        return Objects.requireNonNull(MAPPING.get(value), () -> "Invalid execute state value: " + value);
    }

    private static final Map<Integer, ExecuteState> MAPPING = Enums.toMap(ExecuteState.class, ExecuteState::value);

}
