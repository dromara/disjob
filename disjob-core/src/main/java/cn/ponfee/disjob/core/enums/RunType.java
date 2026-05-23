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
import lombok.Getter;

/**
 * The run type enum definition.
 * <p>mapped by sched_instance.run_type
 *
 * @author Ponfee
 */
@Getter
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

    /**
     * Trigger time dedup key value
     */
    public static final long DEDUP_KEY_VALUE = 0L;

    private final int value;

    /**
     * Trigger time whether is deduplication
     */
    private final boolean dedupByTriggerTime;

    private final String desc;

    RunType(int value, boolean dedupByTriggerTime, String desc) {
        this.value = value;
        this.dedupByTriggerTime = dedupByTriggerTime;
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

    public long getDedupKey() {
        if (dedupByTriggerTime) {
            return DEDUP_KEY_VALUE;
        }
        throw new UnsupportedOperationException(this + " cannot supported dedup key.");
    }

    public static RunType of(int value) {
        for (RunType e : VALUES) {
            if (e.value() == value) {
                return e;
            }
        }
        throw new IllegalArgumentException("Invalid run type value: " + value);
    }

    private static final RunType[] VALUES = RunType.values();

}
