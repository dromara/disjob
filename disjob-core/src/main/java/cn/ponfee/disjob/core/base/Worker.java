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
import cn.ponfee.disjob.core.exception.AuthenticationException;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.core.model.TokenType;
import cn.ponfee.disjob.core.param.worker.AuthenticationParam;
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
import static cn.ponfee.disjob.common.collect.Collects.get;
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
public class Worker extends Server {
    private static final long serialVersionUID = 8981019172872301692L;

    /**
     * Group name
     *
     * @see SchedJob#getGroup()
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

        Assert.hasText(group, "Group cannot be empty.");
        Assert.hasText(workerId, "Worker id cannot be empty.");
        Assert.isTrue(!group.contains(COLON), "Group cannot contains symbol ':'");
        Assert.isTrue(!workerId.contains(COLON), "Worker id cannot contains symbol ':'");
        this.group = group.trim();
        this.workerId = workerId.trim();

        this.serializedValue = this.group + COLON + this.workerId + COLON + super.host + COLON + super.port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Worker) || !super.equals(o)) {
            return false;
        }
        Worker other = (Worker) o;
        return this.group.equals(other.group)
            && this.workerId.equals(other.workerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, workerId, host, port);
    }

    @Override
    public String serialize() {
        return serializedValue;
    }

    public String getGroup() {
        return group;
    }

    public String getWorkerId() {
        return workerId;
    }

    /**
     * 判断当前Worker的group是否匹配任务分派的Worker的group
     *
     * @param group the group
     * @return {@code true} if matched
     */
    public boolean matchesGroup(String group) {
        return this.group.equals(group);
    }

    /**
     * 判断是否同一台机器：其它属性相同但worker-id不同
     * Worker机器宕机重启后，Supervisor一段时间内仍缓存着旧的Worker，导致http通过ip:port派发的任务参数中Worker是旧的，但是新Worker接收该任务
     *
     * @param other the other worker
     * @return {@code true} if same worker
     */
    public boolean sameWorker(Worker other) {
        return super.sameServer(other)
            && this.matchesGroup(other.group);
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
        Assert.hasText(text, "Serialized text cannot empty.");
        String[] array = text.split(COLON);

        String group = get(array, 0);
        Assert.hasText(group, "Worker group cannot bank.");

        String workerId = get(array, 1);
        Assert.hasText(workerId, "Worker id cannot bank.");

        String host = get(array, 2);
        Assert.hasText(host, "Worker host cannot bank.");

        int port = Numbers.toInt(get(array, 3));

        return new Worker(group, workerId, host, port);
    }

    public static Worker.Current current() {
        return Current.instance;
    }

    public static boolean matchesGroup(Worker worker, String group) {
        return worker != null && worker.matchesGroup(group);
    }

    // --------------------------------------------------------custom jackson serialize & deserialize

    /**
     * Custom serialize Worker based jackson.
     */
    public static class JacksonSerializer extends JsonSerializer<Worker> {
        @Override
        public void serialize(Worker value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeString(value.serialize());
            }
        }
    }

    /**
     * Custom deserialize Worker based jackson.
     */
    public static class JacksonDeserializer extends JsonDeserializer<Worker> {
        @Override
        public Worker deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            return Worker.deserialize(p.getText());
        }
    }

    // -------------------------------------------------------------------------------current Worker

    public abstract static class Current extends Worker {
        private static final long serialVersionUID = -480329874106279202L;
        private static volatile Current instance = null;

        private final LocalDateTime startupAt;

        private Current(String group, String workerId, String host, int port) {
            super(group, workerId, host, port);
            SingletonClassConstraint.constrain(Current.class);
            this.startupAt = LocalDateTime.now();
        }

        public final LocalDateTime getStartupAt() {
            return startupAt;
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
         * Worker signature
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

        @Override
        public boolean equals(Object o) {
            return super.equals(o);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        // need do reflection call
        // use synchronized modify for help multiple thread read reference(write to main memory)
        private static synchronized Current create(final String group, final String workerId,
                                                   final String host, final int port,
                                                   final String wToken, final String sToken,
                                                   final String supervisorContextPath) {
            if (instance != null) {
                throw new Error("Current worker already created.");
            }

            instance = new Current(group, workerId, host, port) {
                private static final long serialVersionUID = 7553139562459109482L;

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

                    String tokenSecret = Objects.requireNonNull(Tokens.createAuthentication(workerToken, TokenType.worker, group));
                    return ImmutableMap.of(AUTHENTICATE_HEADER_GROUP, group, AUTHENTICATE_HEADER_TOKEN, tokenSecret);
                }

                @Override
                public String createWorkerSignatureToken() {
                    return Tokens.createSignature(workerToken, TokenType.worker, group);
                }

                @Override
                public void verifySupervisorAuthenticationToken(AuthenticationParam param) {
                    if (!Tokens.verifyAuthentication(param.getSupervisorToken(), supervisorToken, TokenType.supervisor, group)) {
                        throw new AuthenticationException("Authenticate failed.");
                    }
                }
            };

            return instance;
        }

    }

}
