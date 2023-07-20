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
 * The collided strategy enum definition.
 * <p>mapped by sched_job.collided_strategy
 *
 * @author Ponfee
 */
public enum CollidedStrategy implements IntValueEnum<CollidedStrategy> {

    /**
     * 并行执行
     */
    CONCURRENT(1, "并行执行"),

    /**
     * 串行执行
     */
    SERIAL(2, "串行执行"),

    /**
     * 覆盖上次任务（取消上次任务，执行本次任务）
     */
    OVERRIDE(3, "覆盖上次任务"),

    /**
     * 丢弃本次任务（丢弃本次任务，继续执行上次任务）
     */
    DISCARD(4, "丢弃本次任务"),

    ;

    private static final Map<Integer, CollidedStrategy> MAPPING = Enums.toMap(CollidedStrategy.class, CollidedStrategy::value);

    private final int value;
    private final String desc;

    CollidedStrategy(int value, String desc) {
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

    public static CollidedStrategy of(Integer value) {
        return Objects.requireNonNull(MAPPING.get(value), () -> "Invalid collided strategy value: " + value);
    }

}
