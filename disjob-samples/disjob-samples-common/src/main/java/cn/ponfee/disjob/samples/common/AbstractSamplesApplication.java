/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.samples.common;

import cn.ponfee.disjob.common.spring.EnableJacksonDateConfigurer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;

/**
 * Abstract samples application
 * <p>
 * 不能单独使用@ComponentScan("xxx")，这会覆盖SpringBootApplication原本的扫描方式，而不是追加扫描包
 *
 * @author Ponfee
 */
@ComponentScans(@ComponentScan("cn.ponfee.disjob.test.handler"))
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@EnableJacksonDateConfigurer
public abstract class AbstractSamplesApplication {

}
