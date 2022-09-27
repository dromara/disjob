package cn.ponfee.scheduler.core.enums;

import cn.ponfee.scheduler.common.util.Enums;

import java.util.Map;

/**
 * The run type enum definition.
 * <p>mapped by sched_track.run_type
 *
 * @author Ponfee
 */
public enum RunType {

    /**
     * 调度计划
     */
    SCHEDULE(1),

    /**
     * 任务依赖
     */
    DEPEND(2),

    /**
     * 失败重试
     */
    RETRY(3),

    /**
     * 手动触发
     */
    MANUAL(4),

    ;

    private static final Map<Integer, RunType> MAPPING = Enums.toMap(RunType.class, RunType::value);

    private final int value;

    RunType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public boolean equals(Integer value) {
        return value != null && this.value == value;
    }

    public static RunType of(int value) {
        RunType runType = MAPPING.get(value);
        if (runType == null) {
            throw new IllegalArgumentException("Invalid run type: " + value);
        }
        return runType;
    }

}
