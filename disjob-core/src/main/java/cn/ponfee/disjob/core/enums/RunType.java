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
 * The run type enum definition.
 * <p>mapped by sched_instance.run_type
 *
 * @author Ponfee
 */
public enum RunType implements IntValueEnum<RunType> {

    /**
     * 调度计划
     */
    SCHEDULE(1, true, "调度计划"),

    /**
     * 任务依赖
     */
    DEPEND(2, false, "任务依赖"),

    /**
     * 失败重试
     */
    RETRY(3, false, "失败重试"),

    /**
     * 手动触发
     */
    MANUAL(4, true, "手动触发"),

    ;

    public static final long UNIQUE_FLAG = 0L;

    private final int value;
    private final boolean uniqueFlag;
    private final String desc;

    RunType(int value, boolean uniqueFlag, String desc) {
        this.value = value;
        this.uniqueFlag = uniqueFlag;
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

    public boolean isUniqueFlag() {
        return uniqueFlag;
    }

    public static RunType of(Integer value) {
        return Objects.requireNonNull(Const.MAPPING.get(value), () -> "Invalid run type value: " + value);
    }

    private static final class Const {
        private static final Map<Integer, RunType> MAPPING = Enums.toMap(RunType.class, RunType::value);
    }

}
