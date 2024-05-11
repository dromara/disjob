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

package cn.ponfee.disjob.common.spring;

import cn.ponfee.disjob.common.date.JavaUtilDateFormat;
import cn.ponfee.disjob.common.spring.JacksonDateConfigurer.JacksonDateConfiguration;
import cn.ponfee.disjob.common.util.Jsons;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.springframework.context.annotation.Import;
import org.springframework.lang.Nullable;

import java.lang.annotation.*;

/**
 * Enable object mapper configurer
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(JacksonDateConfiguration.class)
public @interface JacksonDateConfigurer {

    class JacksonDateConfiguration {
        public JacksonDateConfiguration(@Nullable ObjectMapper objectMapper) {
            if (objectMapper == null) {
                return;
            }

            objectMapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
            objectMapper.setDateFormat(JavaUtilDateFormat.DEFAULT);
            Jsons.registerSimpleModule(objectMapper);
            Jsons.registerJavaTimeModule(objectMapper);
            objectMapper.registerModule(new Jdk8Module());
        }
    }

}
