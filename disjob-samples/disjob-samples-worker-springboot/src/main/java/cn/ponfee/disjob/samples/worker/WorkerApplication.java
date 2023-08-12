/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.samples.worker;

import cn.ponfee.disjob.samples.common.AbstractSamplesApplication;
import cn.ponfee.disjob.samples.common.util.SampleConstants;
import cn.ponfee.disjob.worker.configuration.EnableWorker;
import org.springframework.boot.SpringApplication;

/**
 * Worker application based spring boot
 *
 * banner:
 *  https://patorjk.com/software/taag/#p=display&h=1&v=1&f=Graffiti&t=Disjob
 *  https://patorjk.com/software/taag/#p=display&h=1&f=Big&t=Disjob
 *
 * @author Ponfee
 */
@EnableWorker
public class WorkerApplication extends AbstractSamplesApplication {

    static {
        // for log4j log file dir
        System.setProperty(SampleConstants.APP_NAME, "worker-springboot");
    }

    public static void main(String[] args) {
        SpringApplication.run(WorkerApplication.class, args);
    }

}
