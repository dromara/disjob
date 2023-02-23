/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.samples.merged;

import cn.ponfee.scheduler.common.base.Snowflake;
import cn.ponfee.scheduler.samples.common.AbstractSchedulerSamplesApplication;
import cn.ponfee.scheduler.samples.common.util.SampleConstants;
import cn.ponfee.scheduler.supervisor.configuration.EnableSupervisor;
import cn.ponfee.scheduler.worker.configuration.EnableWorker;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Import;

/**
 * Scheduler application based spring boot
 *
 * @author Ponfee
 */
@Import(Snowflake.class) // IdGenerator
@EnableSupervisor
@EnableWorker
public class SchedulerApplication extends AbstractSchedulerSamplesApplication {

    static {
        System.setProperty(SampleConstants.APP_NAME, "merged-scheduler");
    }

    public static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
    }

}
