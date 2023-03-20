/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.enums;

import cn.ponfee.scheduler.common.base.IntValue;
import cn.ponfee.scheduler.common.util.Enums;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * The job type enum definition.
 * <p>mapped by sched_job.jb_type
 *
 * @author Ponfee
 */
public enum JobType implements IntValue<JobType> {

    /**
     * 普通
     */
    NORMAL(1),

    /**
     * 广播
     */
    BROADCAST(2),

    /**
     * 工作流(DAG), Unimplemented
     */
    WORKFLOW(3),

    /**
     * 分布式计算(MapReduce), Unimplemented
     */
    MAP_REDUCE(4);

    private static final Map<Integer, JobType> MAPPING = Enums.toMap(JobType.class, JobType::value);

    private final int value;

    JobType(int value) {
        this.value = value;
    }

    @Override
    public int value() {
        return value;
    }

    public static JobType of(Integer value) {
        JobType runType = MAPPING.get(value);
        Assert.notNull(runType, () -> "Invalid job type value: " + value);
        return runType;
    }

}
