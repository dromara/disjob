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
 * The misfire strategy enum definition.
 * <p>mapped by sched_job.misfire_strategy
 *
 * @author Ponfee
 */
public enum MisfireStrategy implements IntValueEnum<MisfireStrategy> {

    /**
     * 触发最近一次misfire
     */
    LAST(1, "触发最近一次"),

    /**
     * 丢弃所有misfire
     */
    DISCARD(2, "丢弃所有"),

    /**
     * 触发所有misfire
     */
    EVERY(3, "触发所有"),

    ;

    private static final Map<Integer, MisfireStrategy> MAPPING = Enums.toMap(MisfireStrategy.class, MisfireStrategy::value);

    private final int value;
    private final String desc;

    MisfireStrategy(int value, String desc) {
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

    public static MisfireStrategy of(Integer value) {
        return Objects.requireNonNull(MAPPING.get(value), () -> "Invalid misfire strategy value: " + value);
    }

}
