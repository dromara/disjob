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

    /**
     * Scan job lock sql
     */
    public static final String LOCK_SQL_SCAN_JOB = "SELECT `name` FROM sched_lock WHERE name='scan_job' FOR UPDATE";

    /**
     * Scan track lock sql
     */
    public static final String LOCK_SQL_SCAN_TRACK = "SELECT `name` FROM sched_lock WHERE name='scan_track' FOR UPDATE";

    /**
     * Spring bean name of scan sched_job table
     */
    public static final String SPRING_BEAN_NAME_SCAN_JOB_LOCKED = "scanJobLocked";

    /**
     * Spring bean name of scan sched_track table
     */
    public static final String SPRING_BEAN_NAME_SCAN_TRACK_LOCKED = "scanTrackLocked";

}
