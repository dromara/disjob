package cn.ponfee.scheduler.supervisor.base;

import cn.ponfee.scheduler.core.base.JobConstants;

/**
 * Supervisor constants definitions.
 *
 * @author Ponfee
 */
public class SupervisorConstants {

    /**
     * Sched job supervisor configuration key prefix.
     */
    public static final String DISTRIBUTED_SCHEDULER_SUPERVISOR = JobConstants.KEY_PREFIX + ".supervisor";

    /**
     * Scan job lock sql
     */
    public static final String LOCK_SQL_SCAN_JOB = "SELECT `name` FROM sched_lock WHERE name='scan_job' FOR UPDATE";

    /**
     * Scan track lock sql
     */
    public static final String LOCK_SQL_SCAN_TRACK = "SELECT `name` FROM sched_lock WHERE name='scan_track' FOR UPDATE";

    /**
     * Job scan time interval seconds
     */
    public static final int SCAN_TIME_INTERVAL_SECONDS = 60;

    public static final String SPRING_BEAN_NAME_CURRENT_SUPERVISOR = "currentSupervisor";

}
