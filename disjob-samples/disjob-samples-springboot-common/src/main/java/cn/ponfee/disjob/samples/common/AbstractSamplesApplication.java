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

/**
 * <pre>
 * Abstract samples application
 *
 * `@ComponentScans(@ComponentScan("xxx"))：会合并SpringBootApplication原本的扫描方式
 * </pre>
 *
 * @author Ponfee
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@ComponentScan({"cn.ponfee.disjob.samples.common", "cn.ponfee.disjob.test.handler"})
@EnableJacksonDateConfigurer // 解决日期反序列化报错的问题
public abstract class AbstractSamplesApplication {

}
