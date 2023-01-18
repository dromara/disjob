/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.base;

/**
 * Supervisor constants definitions.
 *
 * @author Ponfee
 */
public class SupervisorConstants {

    // ----------------------------------------------------------------scan lock sql

    /**
     * Scan triggering job lock sql
     */
    public static final String LOCK_SQL_SCAN_TRIGGERING_JOB = "SELECT `name` FROM sched_lock WHERE name='scan_triggering_job' FOR UPDATE";

    /**
     * Scan waiting track lock sql
     */
    public static final String LOCK_SQL_SCAN_WAITING_TRACK = "SELECT `name` FROM sched_lock WHERE name='scan_waiting_track' FOR UPDATE";

    /**
     * Scan running track lock sql
     */
    public static final String LOCK_SQL_SCAN_RUNNING_TRACK = "SELECT `name` FROM sched_lock WHERE name='scan_running_track' FOR UPDATE";

    // ----------------------------------------------------------------scan locker name

    /**
     * Spring bean name of scan triggering job locker
     */
    public static final String SPRING_BEAN_NAME_SCAN_TRIGGERING_JOB_LOCKER = "scan-triggering-job-locker";

    /**
     * Spring bean name of scan waiting track locker
     */
    public static final String SPRING_BEAN_NAME_SCAN_WAITING_TRACK_LOCKER = "scan-waiting-track-locker";

    /**
     * Spring bean name of scan running track locker
     */
    public static final String SPRING_BEAN_NAME_SCAN_RUNNING_TRACK_LOCKER = "scan-running-track-locker";

}
