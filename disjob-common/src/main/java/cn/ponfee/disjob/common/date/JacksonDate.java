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

package cn.ponfee.disjob.common.date;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
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

    private final JsonSerializer<Date> serializer;
    private final JsonDeserializer<Date> deserializer;

    public JacksonDate(DateFormat format) {
        this.serializer = new Serializer(format);
        this.deserializer = new Deserializer(format);
    }

    public JsonSerializer<Date> serializer() {
        return this.serializer;
    }

    public JsonDeserializer<Date> deserializer() {
        return this.deserializer;
    }

    private static class Serializer extends JsonSerializer<Date> {
        private final DateFormat format;

        private Serializer(DateFormat format) {
            this.format = format;
        }

        @Override
        public void serialize(Date date, JsonGenerator generator, SerializerProvider provider) throws IOException {
            if (date == null) {
                generator.writeNull();
            } else {
                generator.writeString(format.format(date));
            }
        }
    }

    private static class Deserializer extends JsonDeserializer<Date> {
        private final DateFormat format;

        private Deserializer(DateFormat format) {
            this.format = format;
        }

        @Override
        public Date deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            String text = p.getText();
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
