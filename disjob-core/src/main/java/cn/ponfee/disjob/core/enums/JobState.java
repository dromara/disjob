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
 * The job state enum definition.
 * <p>mapped by sched_job.job_state
 *
 * @author Ponfee
 */
public enum JobState implements IntValueEnum<JobState> {

    /**
     * 启用
     */
    ENABLED(1, "启用"),

    /**
     * 禁用
     */
    DISABLED(0, "禁用"),

    ;

    private final int value;
    private final String desc;

    JobState(int value, String desc) {
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

    public static JobState of(int value) {
        for (JobState e : VALUES) {
            if (e.value() == value) {
                return e;
            }
        }
        throw new IllegalArgumentException("Invalid job state value: " + value);
    }

    private static final JobState[] VALUES = JobState.values();

}
