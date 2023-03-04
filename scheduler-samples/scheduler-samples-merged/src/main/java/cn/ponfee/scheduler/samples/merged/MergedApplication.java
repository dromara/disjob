/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.samples.merged;

import cn.ponfee.scheduler.samples.common.AbstractSamplesApplication;
import cn.ponfee.scheduler.samples.common.util.SampleConstants;
import cn.ponfee.scheduler.supervisor.configuration.EnableSupervisor;
import cn.ponfee.scheduler.worker.configuration.EnableWorker;
import org.springframework.boot.SpringApplication;

/**
 * Scheduler application based spring boot
 *
 * @author Ponfee
 */
@EnableSupervisor
@EnableWorker
public class MergedApplication extends AbstractSamplesApplication {

    static {
        // for log4j log file dir
        System.setProperty(SampleConstants.APP_NAME, "merged-scheduler");
    }

    public static void main(String[] args) {
        SpringApplication.run(MergedApplication.class, args);
    }

}
