/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.spring;

import cn.ponfee.disjob.common.date.JavaUtilDateFormat;
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
@Import(EnableJacksonDateConfigurer.ObjectMapperConfigurer.class)
public @interface EnableJacksonDateConfigurer {

    class ObjectMapperConfigurer {

        public ObjectMapperConfigurer(@Nullable ObjectMapper objectMapper) {
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
