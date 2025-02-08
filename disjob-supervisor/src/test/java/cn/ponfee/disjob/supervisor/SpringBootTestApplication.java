/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.supervisor;

import cn.ponfee.disjob.common.base.IdGenerator;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.supervisor.configuration.EnableSupervisor;
import cn.ponfee.disjob.test.EmbeddedMysqlAndRedisServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.atomic.AtomicLong;

/**
 * SpringBootTestApplication
 *
 * @author Ponfee
 */
@EnableSupervisor
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class SpringBootTestApplication {

    static {
        EmbeddedMysqlAndRedisServer.starter()
            .mysqlPort(23306)
            .redisMasterPort(26379)
            .redisSlavePort(26380)
            .start();
        ThrowingRunnable.doChecked(() -> Thread.sleep(5000));
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringBootTestApplication.class, args);
    }

    // 因测试用例为单机模式，可代替`@DbSnowflakeIdGenerator(jdbcTemplateRef = SPRING_BEAN_NAME_JDBC_TEMPLATE)`
    @Bean
    IdGenerator idGenerator() {
        return new IdGenerator() {
            final AtomicLong counter = new AtomicLong(0);

            @Override
            public long generateId() {
                return counter.incrementAndGet();
            }
        };
    }

}
