/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.enums;

import cn.ponfee.scheduler.common.base.IntValueEnum;
import cn.ponfee.scheduler.common.util.Enums;

import java.util.Map;
import java.util.Objects;

/**
 * The route strategy enum definition.
 * <p>mapped by sched_job.route_strategy
 *
 * @author Ponfee
 */
public enum RouteStrategy implements IntValueEnum<RouteStrategy> {

    /**
     * 轮询
     */
    ROUND_ROBIN(1),

    /**
     * 随机
     */
    RANDOM(2),

    /**
     * 简单的哈希
     */
    SIMPLE_HASH(3),

    /**
     * 一致性哈希
     */
    CONSISTENT_HASH(4),

    /**
     * 本地优先(当supervisor同时也是worker角色时生效)
     */
    LOCAL_PRIORITY(5),
    ;

    private static final Map<Integer, RouteStrategy> MAPPING = Enums.toMap(RouteStrategy.class, RouteStrategy::value);

    private final int value;

    RouteStrategy(int value) {
        this.value = value;
    }

    @Override
    public int value() {
        return value;
    }

    public static RouteStrategy of(Integer value) {
        return Objects.requireNonNull(MAPPING.get(value), () -> "Invalid route strategy value: " + value);
    }

}
