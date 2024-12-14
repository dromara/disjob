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
import cn.ponfee.disjob.core.dto.worker.AuthenticationParam;
import cn.ponfee.disjob.core.enums.TokenType;
import cn.ponfee.disjob.core.exception.AuthenticationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import static cn.ponfee.disjob.common.base.Symbol.Str.COLON;
import static cn.ponfee.disjob.core.base.JobConstants.AUTHENTICATE_HEADER_GROUP;
import static cn.ponfee.disjob.core.base.JobConstants.AUTHENTICATE_HEADER_TOKEN;

/**
 * Worker for execute task(JVM instance)
 *
 * @author Ponfee
 * @see com.fasterxml.jackson.annotation.JsonCreator
 * @see com.fasterxml.jackson.annotation.JsonProperty
 */
@JsonSerialize(using = Worker.JacksonSerializer.class)
@JsonDeserialize(using = Worker.JacksonDeserializer.class)
public class Worker extends Server implements Comparable<Worker> {
    private static final long serialVersionUID = 8981019172872301692L;

    /**
     * Group name
     */
    private final String group;

    /**
     * Worker id(a string value of UUID)
     */
    private final String workerId;

    /**
     * Serialized value
     */
    private final transient String serializedValue;

    public Worker(String group, String workerId, String host, int port) {
        super(host, port);
        this.group = check(group);
        this.workerId = check(workerId);

        this.serializedValue = this.group + COLON + this.workerId + COLON + super.host + COLON + super.port;
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, workerId, host, port);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Worker) || !super.equals(obj)) {
            return false;
        }
        Worker that = (Worker) obj;
        return this.group.equals(that.group)
            && this.workerId.equals(that.workerId);
    }

    @Override
    public final String serialize() {
        return serializedValue;
    }

    /**
     * 判断是否同一台机器：其它属性相同，worker-id可以不同
     * Worker机器宕机重启后，Supervisor一段时间内仍缓存着旧的Worker，导致http通过ip:port派发的任务参数中Worker是旧的，但是新Worker接收该任务
     *
     * @param other the other worker
     * @return {@code true} if same worker
     */
    public boolean matches(Worker other) {
        return super.equals(other)
            && this.equalsGroup(other.group);
    }

    /**
     * 判断当前Worker的group是否等于任务分派的Worker的group
     *
     * @param group the group
     * @return {@code true} if matched
     */
    public boolean equalsGroup(String group) {
        return this.group.equals(group);
    }

    public String getGroup() {
        return group;
    }

    public String getWorkerId() {
        return workerId;
    }

    // --------------------------------------------------------static method

    public static Worker deserialize(byte[] bytes, Charset charset) {
        return deserialize(new String(bytes, charset));
    }

    /**
     * Deserialize from string.
     *
     * @param text the serialized text string
     * @return worker object of the text deserialized result
     */
    public static Worker deserialize(String text) {
        String[] array = text.split(COLON, 4);
        Assert.isTrue(array.length == 4, () -> "Invalid worker value: " + text);
        return new Worker(array[0], array[1], array[2], Integer.parseInt(array[3]));
    }

    public static Local local() {
        return Local.instance;
    }

    @Override
    public int compareTo(Worker that) {
        int n = this.workerId.compareTo(that.workerId);
        if (n != 0) {
            return n;
        }
        n = this.host.compareTo(that.host);
        if (n != 0) {
            return n;
        }
        n = Integer.compare(this.port, that.port);
        if (n != 0) {
            return n;
        }
        return this.group.compareTo(that.group);
    }

    // --------------------------------------------------------local Worker

    @SuppressWarnings({"serial", "unused"})
    public abstract static class Local extends Worker {
        private static volatile Local instance = null;

        private final LocalDateTime startupTime;

        private Local(String group, String workerId, String host, int port) {
            super(group, workerId, host, port);
            SingletonClassConstraint.constrain(Local.class);
            this.startupTime = LocalDateTime.now();
        }

        public final LocalDateTime getStartupTime() {
            return startupTime;
        }

        /**
         * Get supervisor context-path
         *
         * @return supervisor context-path
         */
        public abstract String getSupervisorContextPath();

        /**
         * 返回http headers，用于Worker远程调用Supervisor RPC接口的授权信息
         *
         * @return map of authentication http headers
         */
        public abstract Map<String, String> createWorkerAuthenticationHeaders();

        /**
         * Creates worker signature token
         *
         * @return signature string
         */
        public abstract String createWorkerSignatureToken();

        /**
         * 认证Supervisor远程调用Worker RPC接口的授权信息
         *
         * @param param authentication param
         */
        public abstract void verifySupervisorAuthenticationToken(AuthenticationParam param);

        // need do reflection call
        // use synchronized modify for help multiple thread read reference(write to main memory)
        private static synchronized Local create(final String group, final String workerId,
                                                 final String host, final int port,
                                                 final String wToken, final String sToken,
                                                 final String supervisorContextPath) {
            if (instance != null) {
                throw new Error("Local worker already created.");
            }

            instance = new Local(group, workerId, host, port) {
                private final String workerToken     = StringUtils.trim(wToken);
                private final String supervisorToken = StringUtils.trim(sToken);

                @Override
                public String getSupervisorContextPath() {
                    return supervisorContextPath;
                }

                @Override
                public Map<String, String> createWorkerAuthenticationHeaders() {
                    if (StringUtils.isEmpty(workerToken)) {
                        return Collections.singletonMap(AUTHENTICATE_HEADER_GROUP, group);
                    }
                    String workerAuthenticationToken = Tokens.createAuthentication(workerToken, TokenType.worker, group);
                    Assert.hasText(workerAuthenticationToken, "Worker authentication token cannot be blank.");
                    return ImmutableMap.of(AUTHENTICATE_HEADER_GROUP, group, AUTHENTICATE_HEADER_TOKEN, workerAuthenticationToken);
                }

                @Override
                public String createWorkerSignatureToken() {
                    return Tokens.createSignature(workerToken, TokenType.worker, group);
                }

                @Override
                public void verifySupervisorAuthenticationToken(AuthenticationParam param) {
                    if (!Tokens.verifyAuthentication(param.getSupervisorAuthenticationToken(), supervisorToken, TokenType.supervisor, group)) {
                        throw new AuthenticationException("Authenticate failed.");
                    }
                }
            };

            return instance;
        }
    }

    // --------------------------------------------------------custom jackson serialize & deserialize

    /**
     * Custom serialize Worker based jackson.
     */
    static class JacksonSerializer extends JsonSerializer<Worker> {
        @Override
        public void serialize(Worker value, JsonGenerator generator, SerializerProvider provider) throws IOException {
            if (value == null) {
                generator.writeNull();
            } else {
                generator.writeString(value.serialize());
            }
        }
    }

    /**
     * Custom deserialize Worker based jackson.
     */
    static class JacksonDeserializer extends JsonDeserializer<Worker> {
        @Override
        public Worker deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            return Worker.deserialize(p.getText());
        }
    }

}
