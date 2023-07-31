/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.spring;

import cn.ponfee.disjob.common.date.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.context.annotation.Import;
import org.springframework.lang.Nullable;

import java.lang.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Enable object mapper configurer
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(EnableJacksonDateConfigurer.ObjectMapperConfigurer.class)
public @interface EnableJacksonDateConfigurer {

    class ObjectMapperConfigurer {

        public ObjectMapperConfigurer(@Nullable ObjectMapper objectMapper) {
            if (objectMapper != null) {
                objectMapper.setDateFormat(JavaUtilDateFormat.DEFAULT);

                SimpleModule module = new SimpleModule();
                module.addSerializer(Date.class, JacksonDate.INSTANCE.serializer());
                module.addDeserializer(Date.class, JacksonDate.INSTANCE.deserializer());
                objectMapper.registerModule(module);

                // java.time.LocalDateTime
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(Dates.DATE_PATTERN);
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                JavaTimeModule javaTimeModule = new JavaTimeModule();
                javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(LocalDateTimeFormat.PATTERN_11));
                javaTimeModule.addDeserializer(LocalDateTime.class, CustomLocalDateTimeDeserializer.INSTANCE);
                javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
                javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));
                javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
                javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));
                objectMapper.registerModule(javaTimeModule);
            }
        }
    }

}
