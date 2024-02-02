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
 * The route strategy enum definition.
 * <p>mapped by sched_job.route_strategy
 *
 * @author Ponfee
 */
public enum RouteStrategy implements IntValueEnum<RouteStrategy> {

    /**
     * 轮询
     */
    ROUND_ROBIN(1, "轮询"),

    /**
     * 随机
     */
    RANDOM(2, "随机"),

    /**
     * 简单哈希
     */
    SIMPLE_HASH(3, "简单哈希"),

    /**
     * 一致性哈希
     */
    CONSISTENT_HASH(4, "一致性哈希"),

    /**
     * 本地优先(当supervisor同时也是worker角色时生效)
     */
    LOCAL_PRIORITY(5, "本地优先"),

    /**
     * 广播
     */
    BROADCAST(6, "广播"),

    ;

    private final int value;
    private final String desc;

    RouteStrategy(int value, String desc) {
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

    public static RouteStrategy of(Integer value) {
        return Objects.requireNonNull(Const.MAPPING.get(value), () -> "Invalid route strategy value: " + value);
    }

    private static final class Const {
        private static final Map<Integer, RouteStrategy> MAPPING = Enums.toMap(RouteStrategy.class, RouteStrategy::value);
    }

}
