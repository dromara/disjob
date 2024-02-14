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

//
//package cn.ponfee.disjob.samples.common.configuration;
//
//import cn.ponfee.disjob.common.util.Jsons;
//import com.fasterxml.jackson.annotation.JsonInclude;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.lang.Nullable;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
///**
// * Spring web mvc configuration
// *
// * @author Ponfee
// */
//@Configuration
//public class SpringWebConfiguration implements WebMvcConfigurer {
//
//    private final ObjectMapper objectMapper;
//
//    /**
//     * 解决spring-boot-admin-server客户端注册进来时反序列化报错问题
//     * <p>
//     * Cannot construct instance of `de.codecentric.boot.admin.server.domain.values.Registration` (no Creators, like default constructor, exist): cannot deserialize from Object value (no delegate- or property-based Creator)
//     *
//     * @param objectMapper
//     */
//    public SpringWebConfiguration(@Nullable ObjectMapper objectMapper) {
//        if (objectMapper == null) {
//            objectMapper = Jsons.createObjectMapper(JsonInclude.Include.NON_NULL);
//        } else {
//            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
//            Jsons.configObjectMapper(objectMapper);
//        }
//
//        // 这种方式无法解决报错问题
//        //com.fasterxml.jackson.databind.module.SimpleModule simpleModule = new com.fasterxml.jackson.databind.module.SimpleModule();
//        //simpleModule.addSerializer(de.codecentric.boot.admin.server.domain.values.Registration.class, com.fasterxml.jackson.databind.ser.std.ToStringSerializer.instance);
//        //simpleModule.addDeserializer(de.codecentric.boot.admin.server.domain.values.Registration.class, new de.codecentric.boot.admin.server.utils.jackson.RegistrationDeserializer());
//        //objectMapper.registerModule(simpleModule);
//
//        this.objectMapper = objectMapper;
//    }
//
//}
