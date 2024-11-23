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
import cn.ponfee.disjob.core.enums.TokenType;
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

import static cn.ponfee.disjob.common.base.Symbol.Str.COLON;
import static cn.ponfee.disjob.common.collect.Collects.get;

/**
 * Supervisor definition
 *
 * @author Ponfee
 */
@JsonSerialize(using = Supervisor.JacksonSerializer.class)
@JsonDeserialize(using = Supervisor.JacksonDeserializer.class)
public class Supervisor extends Server implements Comparable<Supervisor> {
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

    @Override
    public int compareTo(Supervisor that) {
        int n = this.host.compareTo(that.host);
        if (n != 0) {
            return n;
        }
        return Integer.compare(this.port, that.port);
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

    public interface GroupInfo {

        String getWorkerContextPath(String group);

        String getSupervisorToken(String group);

        String getWorkerToken(String group);

        String getUserToken(String group);
    }

    /**
     * Supervisor.class.getDeclaredClasses()[0]
     */
    @SuppressWarnings({"serial", "unused"})
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

        /**
         * Creates supervisor authentication token
         *
         * @param group the group
         * @return supervisor authentication token
         */
        public abstract String createSupervisorAuthenticationToken(String group);

        /**
         * Verifies worker authentication token
         *
         * @param group                     the group
         * @param workerAuthenticationToken the worker authentication token
         * @return {@code true} if verify successful
         */
        public abstract boolean verifyWorkerAuthenticationToken(String group, String workerAuthenticationToken);

        /**
         * Verifies user authentication token
         *
         * @param group                   the group
         * @param userAuthenticationToken the user authentication token
         * @return {@code true} if verify successful
         */
        public abstract boolean verifyUserAuthenticationToken(String group, String userAuthenticationToken);

        /**
         * Verifies worker signature token
         *
         * @param group                the group
         * @param workerSignatureToken the worker signature token
         * @return {@code true} if verify successful
         */
        public abstract boolean verifyWorkerSignatureToken(String group, String workerSignatureToken);

        private static synchronized Local create(final String host, final int port, final GroupInfo groupInfo) {
            if (instance != null) {
                throw new Error("Local supervisor already created.");
            }

            instance = new Local(host, port) {
                @Override
                public String getWorkerContextPath(String group) {
                    return groupInfo.getWorkerContextPath(group);
                }

                @Override
                public String createSupervisorAuthenticationToken(String group) {
                    String supervisorToken = groupInfo.getSupervisorToken(group);
                    return Tokens.createAuthentication(supervisorToken, TokenType.supervisor, group);
                }

                @Override
                public boolean verifyWorkerAuthenticationToken(String group, String workerAuthenticationToken) {
                    String workerToken = groupInfo.getWorkerToken(group);
                    return Tokens.verifyAuthentication(workerAuthenticationToken, workerToken, TokenType.worker, group);
                }

                @Override
                public boolean verifyUserAuthenticationToken(String group, String userAuthenticationToken) {
                    String userToken = groupInfo.getUserToken(group);
                    return Tokens.verifyAuthentication(userAuthenticationToken, userToken, TokenType.user, group);
                }

                @Override
                public boolean verifyWorkerSignatureToken(String group, String workerSignatureToken) {
                    String workerToken = groupInfo.getWorkerToken(group);
                    return Tokens.verifySignature(workerSignatureToken, workerToken, TokenType.worker, group);
                }
            };

            return instance;
        }
    }

}
