package cn.ponfee.scheduler.core.enums;

import cn.ponfee.scheduler.common.util.Enums;

import java.util.Map;

/**
 * The misfire strategy enum definition.
 * <p>mapped by sched_job.misfire_strategy
 *
 * @author Ponfee
 */
public enum MisfireStrategy {

    /**
     * 触发最近一次
     */
    LAST(1),

    /**
     * 丢弃
     */
    DISCARD(2),

    /**
     * 触发所有
     */
    EVERY(3),

    ;

    private static final Map<Integer, MisfireStrategy> MAPPING = Enums.toMap(MisfireStrategy.class, MisfireStrategy::value);

    private final int value;

    MisfireStrategy(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public boolean equals(Integer value) {
        return value != null && this.value == value;
    }

    public static MisfireStrategy of(int value) {
        MisfireStrategy misfireStrategy = MAPPING.get(value);
        if (misfireStrategy == null) {
            throw new IllegalArgumentException("Invalid misfire strategy: " + value);
        }
        return misfireStrategy;
    }
}
