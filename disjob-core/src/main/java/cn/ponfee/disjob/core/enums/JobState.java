/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.enums;

import cn.ponfee.disjob.common.base.IntValueEnum;

import java.util.Objects;

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
    ENABLE(1, "启用"),

    /**
     * 禁用
     */
    DISABLE(0, "禁用"),

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

    public static JobState of(Integer value) {
        Objects.requireNonNull(value, "Job state value cannot be null.");
        for (JobState state : JobState.values()) {
            if (state.value == value) {
                return state;
            }
        }
        throw new IllegalArgumentException("Invalid job state value: " + value);
    }

}
