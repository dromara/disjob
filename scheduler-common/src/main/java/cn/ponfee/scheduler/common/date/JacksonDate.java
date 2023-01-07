/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.date;

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
 * The Jackson Money Serializer & Deserializer
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
