package cn.ponfee.scheduler.supervisor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * @author Ponfee
 */
@Configuration
public class TestConfig {

    @Bean
    @Order(34)
    public String string1(){
        return "String-1";
    }

    @Bean
    @Order(14)
    public String string2(){
        return "String-2";
    }

}
