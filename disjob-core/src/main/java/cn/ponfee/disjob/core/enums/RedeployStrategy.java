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
 * The redeployed strategy enum definition.
 * <p>mapped by sched_job.redeploy_strategy
 *
 * @author Ponfee
 */
public enum RedeployStrategy implements IntValueEnum<RedeployStrategy> {

    /**
     * 恢复执行
     */
    RESTART(1, Operation.SHUTDOWN_RESTART, "恢复执行"),

    /**
     * 暂停执行
     */
    PAUSE(2, Operation.SHUTDOWN_PAUSE, "暂停执行"),

    /**
     * 取消执行
     */
    CANCEL(3, Operation.SHUTDOWN_CANCEL, "取消执行"),

    ;

    private final int value;
    private final Operation operation;
    private final String desc;

    RedeployStrategy(int value, Operation operation, String desc) {
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

    public static RedeployStrategy of(Integer value) {
        return Objects.requireNonNull(Const.MAPPING.get(value), () -> "Invalid redeploy strategy value: " + value);
    }

    private static final class Const {
        private static final Map<Integer, RedeployStrategy> MAPPING = Enums.toMap(RedeployStrategy.class, RedeployStrategy::value);
    }

}
