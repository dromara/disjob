package cn.ponfee.scheduler.core.enums;

/**
 * The job state enum definition.
 * <p>mapped by sched_job.job_state
 *
 * @author Ponfee
 */
public enum JobState {

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

    public int value() {
        return value;
    }

    public boolean equals(Integer value) {
        return value != null && this.value == value;
    }

    public static JobState of(int value) {
        for (JobState state : JobState.values()) {
            if (state.value == value) {
                return state;
            }
        }
        throw new IllegalArgumentException("Invalid job state: " + value);
    }

}
