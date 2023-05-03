/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.samples.merged;

import cn.ponfee.disjob.samples.common.AbstractSamplesApplication;
import cn.ponfee.disjob.samples.common.util.SampleConstants;
import cn.ponfee.disjob.supervisor.configuration.EnableSupervisor;
import cn.ponfee.disjob.worker.configuration.EnableWorker;
import org.springframework.boot.SpringApplication;

/**
 * Disjob application based spring boot
 *
 * @author Ponfee
 */
@EnableSupervisor
@EnableWorker
public class MergedApplication extends AbstractSamplesApplication {

    static {
        // for log4j log file dir
        System.setProperty(SampleConstants.APP_NAME, "merged-disjob");
    }

    public static void main(String[] args) {
        SpringApplication.run(MergedApplication.class, args);
    }

}
