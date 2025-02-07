/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.supervisor.base;

import static cn.ponfee.disjob.core.base.JobConstants.SPRING_BEAN_NAME_PREFIX;

/**
 * Supervisor constants definitions.
 *
 * @author Ponfee
 */
public class SupervisorConstants {

    // ----------------------------------------------------------------scan lock sql

    /**
     * Scan triggering job lock name
     */
    public static final String LOCK_SCAN_TRIGGERING_JOB = "triggering_job";

    /**
     * Scan waiting instance lock name
     */
    public static final String LOCK_SCAN_WAITING_INSTANCE = "waiting_instance";

    /**
     * Scan running instance lock name
     */
    public static final String LOCK_SCAN_RUNNING_INSTANCE = "running_instance";

    // ----------------------------------------------------------------scan locker spring bean name

    /**
     * Spring bean name of scan waiting instance locker
     */
    public static final String SPRING_BEAN_NAME_SCAN_WAITING_INSTANCE_LOCKER = SPRING_BEAN_NAME_PREFIX + ".scan-waiting-instance-locker";

    /**
     * Spring bean name of scan running instance locker
     */
    public static final String SPRING_BEAN_NAME_SCAN_RUNNING_INSTANCE_LOCKER = SPRING_BEAN_NAME_PREFIX + ".scan-running-instance-locker";

    /**
     * Spring bean name of scan triggering job locker
     */
    public static final String SPRING_BEAN_NAME_SCAN_TRIGGERING_JOB_LOCKER = SPRING_BEAN_NAME_PREFIX + ".scan-triggering-job-locker";

}
