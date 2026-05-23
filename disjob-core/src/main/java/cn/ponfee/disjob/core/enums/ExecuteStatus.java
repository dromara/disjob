/*
 * Copyright 2022-2026 Ponfee (http://www.ponfee.cn/)
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
 * The task executed status enum definition.
 * <p>mapped by sched_task.execute_status
 *
 * @author Ponfee
 */
public enum ExecuteStatus implements IntValueEnum<ExecuteStatus> {

    /**
     * 等待执行
     */
    WAITING(10, RunStatus.WAITING, "等待执行"),

    /**
     * 正在执行
     */
    EXECUTING(20, RunStatus.RUNNING, "正在执行"),

    /**
     * 暂停执行
     */
    PAUSED(30, RunStatus.PAUSED, "暂停执行"),

    /**
     * 执行完成
     */
    COMPLETED(40, RunStatus.COMPLETED, "执行完成"),

    /**
     * 派发失败取消(任务派发失败的次数sched_task.dispatch_failures > SupervisorProperties#maximumTaskDispatchFailures)
     */
    DISPATCH_FAILED(50, RunStatus.CANCELED, "派发失败"),

    /**
     * 初始化异常取消
     */
    INITIALIZE_EXCEPTION(51, RunStatus.CANCELED, "初始化异常"),

    /**
     * 执行失败取消(`JobExecutor#execute`方法的返回结果为null或code!=0)
     */
    EXECUTE_FAILED(52, RunStatus.CANCELED, "执行失败"),

    /**
     * 执行异常取消
     */
    EXECUTE_EXCEPTION(53, RunStatus.CANCELED, "执行异常"),

    /**
     * 执行超时取消(任务的执行耗时 > sched_job.execute_timeout > 0)
     */
    EXECUTE_TIMEOUT(54, RunStatus.CANCELED, "执行超时"),

    /**
     * 执行终止(如执行过程中Worker宕机)
     */
    EXECUTE_ABORTED(55, RunStatus.CANCELED, "执行终止"),

    /**
     * 广播任务终止(广播任务分派的Worker已下线导致未执行)
     */
    BROADCAST_ABORTED(56, RunStatus.CANCELED, "广播终止"),

    /**
     * 执行冲突取消(sched_job.collision_strategy=3 并且 上次任务还未执行完成 时取消上次任务)
     */
    COLLISION_CANCELED(57, RunStatus.CANCELED, "冲突取消"),

    /**
     * Worker关机取消(sched_job.shutdown_strategy=3 并且 Worker正常关闭 时被取消)
     */
    SHUTDOWN_CANCELED(58, RunStatus.CANCELED, "关机取消"),

    /**
     * 手动取消
     */
    MANUAL_CANCELED(59, RunStatus.CANCELED, "手动取消"),

    ;

    /**
     * Status value
     */
    private final int value;

    /**
     * mapped sched_instance.run_status
     */
    private final RunStatus runStatus;

    /**
     * Description
     */
    private final String desc;

    ExecuteStatus(int value, RunStatus runStatus, String desc) {
        this.value = value;
        this.runStatus = runStatus;
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

    public RunStatus runStatus() {
        return runStatus;
    }

    public boolean isPausable() {
        return this == WAITING || this == EXECUTING;
    }

    public boolean isTerminal() {
        return runStatus.isTerminal();
    }

    public boolean isFailure() {
        return runStatus.isFailure();
    }

    public static ExecuteStatus of(Integer value) {
        return Objects.requireNonNull(MAPPING.get(value), () -> "Invalid execute status value: " + value);
    }

    private static final Map<Integer, ExecuteStatus> MAPPING = Enums.toMap(ExecuteStatus.class, ExecuteStatus::value);

}
