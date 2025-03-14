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
import cn.ponfee.disjob.supervisor.configuration.EnableSupervisor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Spring boot test application
 *
 * @author Ponfee
 */
@EnableSupervisor
@SpringBootApplication
public class SpringBootTestApplication {

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
