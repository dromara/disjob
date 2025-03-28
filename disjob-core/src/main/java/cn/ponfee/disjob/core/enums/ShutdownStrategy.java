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
 * The shutdown strategy enum definition.
 * <p>mapped by sched_job.shutdown_strategy
 *
 * @author Ponfee
 */
public enum ShutdownStrategy implements IntValueEnum<ShutdownStrategy> {

    /**
     * 恢复执行：Worker优雅关闭时，停止当前Worker中所有正在执行的task，将状态更改为`WAITING`，等待一段时间后会自动转移到其它的Worker上执行
     */
    RESUME(1, Operation.SHUTDOWN_RESUME, "恢复执行"),

    /**
     * 暂停执行：Worker优雅关闭时，暂停当前Worker中所有正在执行的task，将状态更改为`PAUSED`
     */
    PAUSE(2, Operation.SHUTDOWN_PAUSE, "暂停执行"),

    /**
     * 取消执行：Worker优雅关闭时，取消当前Worker中所有正在执行的task，将状态更改为`SHUTDOWN_CANCELED`
     */
    CANCEL(3, Operation.SHUTDOWN_CANCEL, "取消执行"),

    ;

    private final int value;
    private final Operation operation;
    private final String desc;

    ShutdownStrategy(int value, Operation operation, String desc) {
        this.value = value;
        this.operation = operation;
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

    public Operation operation() {
        return operation;
    }

    public static ShutdownStrategy of(int value) {
        for (ShutdownStrategy e : VALUES) {
            if (e.value() == value) {
                return e;
            }
        }
        throw new IllegalArgumentException("Invalid shutdown strategy value: " + value);
    }

    private static final ShutdownStrategy[] VALUES = ShutdownStrategy.values();

}
