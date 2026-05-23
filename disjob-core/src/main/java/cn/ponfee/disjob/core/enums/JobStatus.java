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

/**
 * The job status enum definition.
 * <p>mapped by sched_job.job_status
 *
 * @author Ponfee
 */
public enum JobStatus implements IntValueEnum<JobStatus> {

    /**
     * 已禁用
     */
    DISABLED(0, "已禁用"),

    /**
     * 已启用
     */
    ENABLED(1, "已启用"),

    ;

    private final int value;
    private final String desc;

    JobStatus(int value, String desc) {
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

    public static JobStatus of(int value) {
        for (JobStatus e : VALUES) {
            if (e.value() == value) {
                return e;
            }
        }
        throw new IllegalArgumentException("Invalid job status value: " + value);
    }

    private static final JobStatus[] VALUES = JobStatus.values();

}
