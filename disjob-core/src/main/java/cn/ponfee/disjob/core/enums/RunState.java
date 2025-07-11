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

/**
 * The run state enum definition.
 * <p>mapped by sched_instance.run_state
 *
 * @author Ponfee
 */
public enum RunState implements IntValueEnum<RunState> {

    /**
     * 待运行
     */
    WAITING(10, false, false, "待运行"),

    /**
     * 运行中
     */
    RUNNING(20, false, false, "运行中"),

    /**
     * 已暂停
     */
    PAUSED(30, false, false, "已暂停"),

    /**
     * 已完成
     */
    COMPLETED(40, true, false, "已完成"),

    /**
     * 已取消
     */
    CANCELED(50, true, true, "已取消"),

    ;

    /**
     * State value
     */
    private final int value;

    /**
     * Whether is terminal state
     */
    private final boolean terminal;

    /**
     * Whether is failure state
     */
    private final boolean failure;

    /**
     * Description
     */
    private final String desc;

    RunState(int value, boolean terminal, boolean failure, String desc) {
        this.value = value;
        this.terminal = terminal;
        this.failure = failure;
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

    public boolean isPausable() {
        return this == WAITING || this == RUNNING;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public boolean isFailure() {
        return failure;
    }

    public static RunState of(int value) {
        for (RunState e : VALUES) {
            if (e.value() == value) {
                return e;
            }
        }
        throw new IllegalArgumentException("Invalid run state value: " + value);
    }

    private static final RunState[] VALUES = RunState.values();

}
