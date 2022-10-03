package cn.ponfee.scheduler.supervisor.base;

/**
 * Supervisor constants definitions.
 *
 * @author Ponfee
 */
public class SupervisorConstants {

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

    /**
     * Spring bean name of scan sched_job table
     */
    public static final String SPRING_BEAN_NAME_SCAN_JOB_LOCKED = "scanJobLocked";

    /**
     * Spring bean name of scan sched_track table
     */
    public static final String SPRING_BEAN_NAME_SCAN_TRACK_LOCKED = "scanTrackLocked";

}
