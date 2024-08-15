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
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.UnaryOperator;

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

    private final transient String serializedValue;

    public Supervisor(String host, int port) {
        super(host, port);

        this.serializedValue = super.host + COLON + super.port;
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public final String serialize() {
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

    public static Local local() {
        return Local.instance;
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

    // -------------------------------------------------------------------------------local Supervisor

    /**
     * Supervisor.class.getDeclaredClasses()[0]
     */
    @SuppressWarnings("serial")
    public abstract static class Local extends Supervisor {
        private static volatile Local instance = null;

        private final LocalDateTime startupTime;

        private Local(String host, int port) {
            super(host, port);
            SingletonClassConstraint.constrain(Local.class);
            this.startupTime = LocalDateTime.now();
        }

        public final LocalDateTime getStartupTime() {
            return startupTime;
        }

        /**
         * Get worker context-path
         *
         * @param group the group
         * @return worker context-path
         */
        public abstract String getWorkerContextPath(String group);

        private static synchronized Local create(final String host, final int port,
                                                 final UnaryOperator<String> workerContextPath) {
            if (instance != null) {
                throw new Error("Local supervisor already created.");
            }

            instance = new Local(host, port) {
                @Override
                public String getWorkerContextPath(String group) {
                    return workerContextPath.apply(group);
                }
            };

            return instance;
        }
    }

}
