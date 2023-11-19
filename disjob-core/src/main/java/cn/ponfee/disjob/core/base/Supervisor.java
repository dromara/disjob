/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.base;

import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.util.Numbers;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.Objects;

import static cn.ponfee.disjob.common.base.Symbol.Str.COLON;
import static cn.ponfee.disjob.common.collect.Collects.get;

/**
 * Supervisor definition
 *
 * @author Ponfee
 */
@JsonSerialize(using = Supervisor.JacksonSerializer.class)
@JsonDeserialize(using = Supervisor.JacksonDeserializer.class)
public class Supervisor extends Server {
    private static final long serialVersionUID = -1254559108807415145L;

    private transient final String serializedValue;

    public Supervisor(String host, int port) {
        super(host, port);
        this.serializedValue = host + COLON + port;
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public String serialize() {
        return serializedValue;
    }

    // --------------------------------------------------------static method

    /**
     * Deserialize from string.
     *
     * @param text the serialized text string
     * @return supervisor object of the text deserialized result
     */
    public static Supervisor deserialize(String text) {
        Assert.hasText(text, "Serialized text cannot empty.");
        String[] array = text.split(COLON);

        String host = get(array, 0);
        Assert.hasText(host, "Supervisor host cannot bank.");

        int port = Numbers.toInt(get(array, 1));

        return new Supervisor(host, port);
    }

    public static Supervisor.Current current() {
        return Current.instance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return (o instanceof Supervisor) && super.equals(o);
    }

    // --------------------------------------------------------custom jackson serialize & deserialize

    /**
     * Custom serialize Supervisor based jackson.
     */
    public static class JacksonSerializer extends JsonSerializer<Supervisor> {
        @Override
        public void serialize(Supervisor value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeString(value.serialize());
            }
        }
    }

    /**
     * Custom deserialize Supervisor based jackson.
     */
    public static class JacksonDeserializer extends JsonDeserializer<Supervisor> {
        @Override
        public Supervisor deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            return Supervisor.deserialize(p.getText());
        }
    }

    // -------------------------------------------------------------------------------class

    /**
     * Supervisor.class.getDeclaredClasses()[0]
     */
    public static abstract class Current extends Supervisor {
        private static final long serialVersionUID = -239845054171219365L;
        private static volatile Current instance;

        private Current(String host, int port) {
            super(host, port);
            SingletonClassConstraint.constrain(Current.class);
        }

        private static synchronized Current create(String host, int port) {
            if (instance != null) {
                throw new Error("Current supervisor already set.");
            }

            instance = new Current(host, port) {
                private static final long serialVersionUID = 3856221643026735022L;
            };

            return instance;
        }
    }

}
