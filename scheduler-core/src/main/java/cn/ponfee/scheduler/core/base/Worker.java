/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.base;

import cn.ponfee.scheduler.common.util.GenericUtils;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.common.util.Numbers;
import cn.ponfee.scheduler.common.util.ObjectUtils;
import com.alibaba.fastjson.annotation.JSONType;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.springframework.util.Assert;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

import static cn.ponfee.scheduler.common.base.Constants.COLON;
import static cn.ponfee.scheduler.common.util.Collects.get;

/**
 * Worker(JVM instance)
 *
 * @author Ponfee
 */
@JSONType(deserializer = Worker.FastjsonDeserializer.class) // fastjson
@JsonDeserialize(using = Worker.JacksonDeserializer.class)  // jackson
public final class Worker extends Server {
    private static final long serialVersionUID = 8981019172872301692L;

    /**
     * Group name, @see SchedJob#getJobGroup()
     */
    private final String group;

    /**
     * Instance id(Auto generate when class {@code Constants} loaded)
     */
    private final String instanceId;

    private transient final String serializedValue;

    public Worker(String group, String instanceId, String host, int port) {
        super(host, port);

        Assert.isTrue(!instanceId.contains(COLON), "Instance-id cannot contains symbol ':'");
        Assert.isTrue(!group.contains(COLON), "Group name cannot contains symbol ':'");
        this.group = group;
        this.instanceId = instanceId;

        this.serializedValue = group + COLON + instanceId + COLON + host + COLON + port;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o)
            && Objects.equals(group, ((Worker) o).group)
            && Objects.equals(instanceId, ((Worker) o).instanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, instanceId, host, port);
    }

    @Override
    public String serialize() {
        return serializedValue;
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

        String instanceId = get(array, 1);
        Assert.hasText(instanceId, "Worker instance-id cannot bank.");

        String host = get(array, 2);
        Assert.hasText(instanceId, "Worker host cannot bank.");

        int port = Numbers.toInt(get(array, 3));

        return new Worker(group, instanceId, host, port);
    }

    public boolean matches(String taskGroup) {
        // Strings.matches(taskGroup, group);
        // new org.springframework.util.AntPathMatcher();
        return group.equals(taskGroup);
    }

    public String getGroup() {
        return group;
    }

    public String getInstanceId() {
        return instanceId;
    }

    // --------------------------------------------------------current worker

    public static Worker current() {
        return Current.current;
    }

    // -----------------------------------------------------custom fastjson deserialize

    /**
     * Custom deserialize Worker based fastjson.
     */
    public static class FastjsonDeserializer implements ObjectDeserializer {
        @Override
        public Worker deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
            if (GenericUtils.getRawType(type) != Worker.class) {
                throw new UnsupportedOperationException("Cannot supported deserialize type: " + type);
            }
            return castToWorker(parser.parseObject());
        }

        @Override
        public int getFastMatchToken() {
            return 0 /*JSONToken.RBRACKET*/;
        }
    }

    // -----------------------------------------------------custom jackson deserialize

    /**
     * Custom deserialize Worker based jackson.
     */
    public static class JacksonDeserializer extends JsonDeserializer<Worker> {
        @Override
        public Worker deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            return castToWorker(p.readValueAs(Jsons.MAP_NORMAL));
        }
    }

    public static Worker castToWorker(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        String group = ObjectUtils.cast(map.get("group"), String.class);
        String instanceId = ObjectUtils.cast(map.get("instanceId"), String.class);
        String host = ObjectUtils.cast(map.get("host"), String.class);
        int port = ObjectUtils.cast(map.get("port"), int.class);
        return new Worker(group, instanceId, host, port);
    }

    /**
     * Holder the current worker context.
     */
    private static class Current {
        private static Worker current;

        // need to use reflection do set
        // use synchronized modify for help multiple thread read reference(write to main memory)
        private static synchronized void set(Worker worker) {
            if (worker == null) {
                throw new AssertionError("Current worker cannot set null.");
            }
            if (current != null) {
                throw new AssertionError("Current worker already set.");
            }
            current = worker;
        }
    }

}
