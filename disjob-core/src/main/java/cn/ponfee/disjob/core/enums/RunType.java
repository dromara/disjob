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
 * The run type enum definition.
 * <p>mapped by sched_instance.run_type
 *
 * @author Ponfee
 */
public enum RunType implements IntValueEnum<RunType> {

    /**
     * 调度计划
     */
    SCHEDULE(1, "调度计划"),

    /**
     * 任务依赖
     */
    DEPEND(2, "任务依赖"),

    /**
     * 失败重试
     */
    RETRY(3, "失败重试"),

    /**
     * 手动触发
     */
    MANUAL(4, "手动触发"),

    ;

    private final int value;
    private final String desc;

    RunType(int value, String desc) {
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

    public static RunType of(Integer value) {
        return Objects.requireNonNull(Const.MAPPING.get(value), () -> "Invalid run type value: " + value);
    }

    private static final class Const {
        private static final Map<Integer, RunType> MAPPING = Enums.toMap(RunType.class, RunType::value);
    }

}
