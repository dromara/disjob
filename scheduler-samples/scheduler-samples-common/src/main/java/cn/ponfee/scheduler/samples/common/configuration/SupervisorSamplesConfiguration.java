/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.samples.common.configuration;

import cn.ponfee.scheduler.common.base.IdGenerator;
import cn.ponfee.scheduler.common.base.Snowflake;
import cn.ponfee.scheduler.core.base.Supervisor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Supervisor Samples Configuration
 *
 * @author Ponfee
 */
@ConditionalOnBean(Supervisor.class)
@Configuration
public class SupervisorSamplesConfiguration {

    @ConditionalOnMissingBean
    @Bean
    public IdGenerator idGenerator() {
        return new Snowflake();
    }
}
