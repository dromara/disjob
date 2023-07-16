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
     * 并行
     */
    CONCURRENT(1),

    /**
     * 串行
     */
    SERIAL(2),

    /**
     * 覆盖上一次（取消上一次任务，并执行当前任务）
     */
    OVERRIDE(3),

    /**
     * 丢弃当前任务
     */
    DISCARD(4),

    ;

    private static final Map<Integer, CollidedStrategy> MAPPING = Enums.toMap(CollidedStrategy.class, CollidedStrategy::value);

    private final int value;

    CollidedStrategy(int value) {
        this.value = value;
    }

    @Override
    public int value() {
        return value;
    }

    public static CollidedStrategy of(Integer value) {
        return Objects.requireNonNull(MAPPING.get(value), () -> "Invalid collided strategy value: " + value);
    }

}
