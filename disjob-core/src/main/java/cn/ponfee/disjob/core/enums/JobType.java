/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.enums;

import cn.ponfee.disjob.common.base.IntValueEnum;
import cn.ponfee.disjob.common.util.Enums;

import java.util.Map;
import java.util.Objects;

/**
 * The job type enum definition.
 * <p>mapped by sched_job.jb_type
 *
 * @author Ponfee
 */
public enum JobType implements IntValueEnum<JobType> {

    /**
     * 普通
     */
    NORMAL(1, "普通(Normal)"),

    /**
     * 工作流(DAG)
     */
    WORKFLOW(2, "工作流(DAG)"),

    ;

    private static final Map<Integer, JobType> MAPPING = Enums.toMap(JobType.class, JobType::value);

    private final int value;
    private final String desc;

    JobType(int value, String desc) {
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

    public static JobType of(Integer value) {
        return Objects.requireNonNull(MAPPING.get(value), () -> "Invalid job type value: " + value);
    }

}
