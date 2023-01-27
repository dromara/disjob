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
     * Scan waiting instance lock sql
     */
    public static final String LOCK_SQL_SCAN_WAITING_INSTANCE = "SELECT `name` FROM sched_lock WHERE name='scan_waiting_instance' FOR UPDATE";

    /**
     * Scan running instance lock sql
     */
    public static final String LOCK_SQL_SCAN_RUNNING_INSTANCE = "SELECT `name` FROM sched_lock WHERE name='scan_running_instance' FOR UPDATE";

    // ----------------------------------------------------------------scan locker name

    /**
     * Spring bean name of scan triggering job locker
     */
    public static final String SPRING_BEAN_NAME_SCAN_TRIGGERING_JOB_LOCKER = "scan-triggering-job-locker";

    /**
     * Spring bean name of scan waiting instance locker
     */
    public static final String SPRING_BEAN_NAME_SCAN_WAITING_INSTANCE_LOCKER = "scan-waiting-instance-locker";

    /**
     * Spring bean name of scan running instance locker
     */
    public static final String SPRING_BEAN_NAME_SCAN_RUNNING_INSTANCE_LOCKER = "scan-running-instance-locker";

}
