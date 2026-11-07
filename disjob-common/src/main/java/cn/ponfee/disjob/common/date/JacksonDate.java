/*
 * Copyright 2022-2026 Ponfee (http://www.ponfee.cn/)
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

package cn.ponfee.disjob.common.date;

import org.apache.commons.lang3.StringUtils;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

/**
 * The jackson Serializer & Deserializer for {@link java.util.Date}
 *
 * @author Ponfee
 */
public class JacksonDate {

    public static final JacksonDate INSTANCE = new JacksonDate(JavaUtilDateFormat.DEFAULT);

    private final ValueSerializer<Date> serializer;
    private final ValueDeserializer<Date> deserializer;

    public JacksonDate(DateFormat format) {
        this.serializer = new Serializer(format);
        this.deserializer = new Deserializer(format);
    }

    public ValueSerializer<Date> serializer() {
        return this.serializer;
    }

    public ValueDeserializer<Date> deserializer() {
        return this.deserializer;
    }

    private static class Serializer extends ValueSerializer<Date> {
        private final DateFormat format;

        private Serializer(DateFormat format) {
            this.format = format;
        }

        @Override
        public void serialize(Date value, JsonGenerator generator, SerializationContext provider) {
            if (value == null) {
                generator.writeNull();
            } else {
                generator.writeString(format.format(value));
            }
        }
    }

    private static class Deserializer extends ValueDeserializer<Date> {
        private final DateFormat format;

        private Deserializer(DateFormat format) {
            this.format = format;
        }

        @Override
        public Date deserialize(JsonParser p, DeserializationContext ctx) {
            String text = p.getString();
            if (StringUtils.isBlank(text)) {
                return null;
            }

            try {
                return format.parse(text);
            } catch (ParseException e) {
                throw new IllegalArgumentException("Invalid date format: " + text);
            }
        }
    }

}
