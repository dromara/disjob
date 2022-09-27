package cn.ponfee.scheduler.core.enums;

import cn.ponfee.scheduler.common.util.Enums;

import java.util.Map;

/**
 * The collision strategy enum definition.
 * <p>mapped by sched_job.collision_strategy
 *
 * @author Ponfee
 */
public enum CollisionStrategy {

    /**
     * 并行
     */
    CONCURRENT(1),

    /**
     * 串行
     */
    SERIAL(2),

    /**
     * 覆盖
     */
    OVERRIDE(3),

    /**
     * 丢弃
     */
    DISCARD(4),

    ;

    private static final Map<Integer, CollisionStrategy> MAPPING = Enums.toMap(CollisionStrategy.class, CollisionStrategy::value);

    private final int value;

    CollisionStrategy(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public boolean equals(Integer value) {
        return value != null && this.value == value;
    }

    public static CollisionStrategy of(int value) {
        CollisionStrategy collisionStrategy = MAPPING.get(value);
        if (collisionStrategy == null) {
            throw new IllegalArgumentException("Invalid collision strategy: " + value);
        }
        return collisionStrategy;
    }
}
