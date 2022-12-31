package cn.ponfee.scheduler.core.enums;

import cn.ponfee.scheduler.common.util.Enums;
import cn.ponfee.scheduler.common.base.IntValue;

import java.util.Map;

/**
 * The misfire strategy enum definition.
 * <p>mapped by sched_job.misfire_strategy
 *
 * @author Ponfee
 */
public enum MisfireStrategy implements IntValue<MisfireStrategy> {

    /**
     * 触发最近一次misfire
     */
    LAST(1),

    /**
     * 丢弃所有misfire
     */
    DISCARD(2),

    /**
     * 触发所有misfire
     */
    EVERY(3),

    ;

    private static final Map<Integer, MisfireStrategy> MAPPING = Enums.toMap(MisfireStrategy.class, MisfireStrategy::value);

    private final int value;

    MisfireStrategy(int value) {
        this.value = value;
    }

    @Override
    public int value() {
        return value;
    }

    public static MisfireStrategy of(Integer value) {
        if (value == null) {
            throw new IllegalArgumentException("Misfire strategy cannot be null.");
        }
        MisfireStrategy misfireStrategy = MAPPING.get(value);
        if (misfireStrategy == null) {
            throw new IllegalArgumentException("Invalid misfire strategy: " + value);
        }
        return misfireStrategy;
    }
}
