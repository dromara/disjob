/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor;

import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.supervisor.configuration.EnableSupervisor;
import cn.ponfee.disjob.test.EmbeddedMysqlAndRedisServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * SpringBootTestApplication
 *
 * @author Ponfee
 */
@EnableSupervisor
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class SpringBootTestApplication {

    static {
        EmbeddedMysqlAndRedisServer.starter().start();
        ThrowingRunnable.caught(() -> Thread.sleep(5000));
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringBootTestApplication.class, args);
    }

}
