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

    public static RetryType of(Integer value) {
        return Objects.requireNonNull(Const.MAPPING.get(value), () -> "Invalid retry type value: " + value);
    }

    private static final class Const {
        private static final Map<Integer, RetryType> MAPPING = Enums.toMap(RetryType.class, RetryType::value);
    }

}
