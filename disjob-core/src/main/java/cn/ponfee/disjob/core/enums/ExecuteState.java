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
import com.google.common.collect.ImmutableList;

import java.util.List;
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
    FINISHED(40, RunState.FINISHED, "执行完成"),

    /**
     * 派发失败
     */
    DISPATCH_FAILED(50, RunState.CANCELED, "派发失败"),

    /**
     * 实例化失败取消
     */
    INSTANCE_FAILED(51, RunState.CANCELED, "实例化异常"),

    /**
     * 校验失败取消
     */
    VERIFY_FAILED(52, RunState.CANCELED, "校验失败"),

    /**
     * 初始化异常取消
     */
    INIT_EXCEPTION(53, RunState.CANCELED, "初始化异常"),

    /**
     * 执行失败取消
     */
    EXECUTE_FAILED(54, RunState.CANCELED, "执行失败"),

    /**
     * 执行异常取消
     */
    EXECUTE_EXCEPTION(55, RunState.CANCELED, "执行异常"),

    /**
     * 执行超时取消
     */
    EXECUTE_TIMEOUT(56, RunState.CANCELED, "执行超时"),

    /**
     * 执行冲突取消(sched_job.collided_strategy=3)
     */
    EXECUTE_COLLIDED(57, RunState.CANCELED, "执行冲突"),

    /**
     * 手动取消
     */
    MANUAL_CANCELED(58, RunState.CANCELED, "手动取消"),

    /**
     * 广播未执行取消(分派的worker机器消亡)
     */
    BROADCAST_ABORTED(59, RunState.CANCELED, "广播未执行"),
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

    public boolean isTerminal() {
        return runState.isTerminal();
    }

    public boolean isFailure() {
        return runState.isFailure();
    }

    public RunState runState() {
        return runState;
    }

    public static ExecuteState of(Integer value) {
        return Objects.requireNonNull(Const.MAPPING.get(value), () -> "Invalid execute state value: " + value);
    }

    public static final class Const {
        private static final Map<Integer, ExecuteState> MAPPING = Enums.toMap(ExecuteState.class, ExecuteState::value);

        /**
         * State list of can transit to PAUSED
         */
        public static final List<ExecuteState> PAUSABLE_LIST = ImmutableList.of(WAITING, EXECUTING);

        /**
         * State list of can transit to EXECUTING
         */
        public static final List<ExecuteState> EXECUTABLE_LIST = ImmutableList.of(WAITING, PAUSED);

        /**
         * State list of can transit to terminated
         *
         * @see #isTerminal()
         */
        public static final List<ExecuteState> TERMINABLE_LIST = ImmutableList.of(WAITING, EXECUTING, PAUSED);
    }

}
