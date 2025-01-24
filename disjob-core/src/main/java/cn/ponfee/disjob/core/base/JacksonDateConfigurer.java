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

package cn.ponfee.disjob.core.base;

import cn.ponfee.disjob.common.date.JavaUtilDateFormat;
import cn.ponfee.disjob.common.util.Jsons;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Jackson date configurer
 * <p>解决`json`参数反序列化时Date的解析问题，如果是`form-data`等形式的参数则需要使用`BaseController`
 *
 * @author Ponfee
 */
class JacksonDateConfigurer {

    static class Primary {
        Primary(ObjectMapper objectMapper) {
            configurer(objectMapper);
        }
    }

    static class Multiple {
        Multiple(List<ObjectMapper> list) {
            if (list != null) {
                list.forEach(JacksonDateConfigurer::configurer);
            }
        }
    }

    private static void configurer(ObjectMapper objectMapper) {
        if (objectMapper != null) {
            objectMapper.setDateFormat(JavaUtilDateFormat.DEFAULT);
            Jsons.registerSimpleModule(objectMapper);
            Jsons.registerJavaTimeModule(objectMapper);
        }
    }

}
