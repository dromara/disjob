package cn.ponfee.scheduler.samples.configuration;

import cn.ponfee.scheduler.common.base.IdGenerator;
import cn.ponfee.scheduler.common.base.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration
public class SamplesConfigurateion {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public IdGenerator idGenerator() {
        return new SnowflakeIdGenerator(1);
    }

}
