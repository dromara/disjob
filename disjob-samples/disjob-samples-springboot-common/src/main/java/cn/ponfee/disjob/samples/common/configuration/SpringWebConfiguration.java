///* __________              _____                                                *\
//** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
//**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
//**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
//**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
//**                      \/          \/     \/                                   **
//\*                                                                              */
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
