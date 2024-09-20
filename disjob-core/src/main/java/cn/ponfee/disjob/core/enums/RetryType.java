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
 * The retry type enum definition.
 * <p>mapped by sched_job.retry_type
 *
 * @author Ponfee
 */
public enum RetryType implements IntValueEnum<RetryType> {

    /**
     * 不重试
     */
    NONE(0, "不重试"),

    /**
     * 只重试失败的Task(copy previous failed task param)
     */
    FAILED(1, "重试失败任务"),

    /**
     * 重试所有的Task(re-split job param to task param)
     */
    ALL(2, "重试所有任务"),

    ;

    private final int value;
    private final String desc;

    RetryType(int value, String desc) {
        this.value = value;
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

    public static RetryType of(int value) {
        for (RetryType e : VALUES) {
            if (e.value() == value) {
                return e;
            }
        }
        throw new IllegalArgumentException("Invalid retry type value: " + value);
    }

    private static final RetryType[] VALUES = RetryType.values();

}
