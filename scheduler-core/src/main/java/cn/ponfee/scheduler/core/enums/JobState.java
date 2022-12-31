package cn.ponfee.scheduler.core.enums;

import cn.ponfee.scheduler.common.base.IntValue;

/**
 * The job state enum definition.
 * <p>mapped by sched_job.job_state
 *
 * @author Ponfee
 */
public enum JobState implements IntValue<JobState> {

    /**
     * 已停止
     */
    STOPPED(0),

    /**
     * 已启动
     */
    STARTED(1),

    ;

    private final int value;

    JobState(int value) {
        this.value = value;
    }

    @Override
    public int value() {
        return value;
    }

    public static JobState of(Integer value) {
        if (value == null) {
            throw new IllegalArgumentException("Job state cannot be null.");
        }
        for (JobState state : JobState.values()) {
            if (state.value == value) {
                return state;
            }
        }
        throw new IllegalArgumentException("Invalid job state: " + value);
    }

}
