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
 * The run type enum definition.
 * <p>mapped by sched_instance.run_type
 *
 * @author Ponfee
 */
public enum RunType implements IntValue<RunType> {

    /**
     * 调度计划
     */
    SCHEDULE(1),

    /**
     * 任务依赖
     */
    DEPEND(2),

    /**
     * 失败重试
     */
    RETRY(3),

    /**
     * 手动触发
     */
    MANUAL(4),

    ;

    private static final Map<Integer, RunType> MAPPING = Enums.toMap(RunType.class, RunType::value);

    private final int value;

    RunType(int value) {
        this.value = value;
    }

    @Override
    public int value() {
        return value;
    }

    public static RunType of(Integer value) {
        RunType runType = MAPPING.get(value);
        Assert.notNull(runType, () -> "Invalid run type value: " + value);
        return runType;
    }

}
