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
import cn.ponfee.scheduler.core.model.SchedJob;
import com.alibaba.fastjson.annotation.JSONType;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import static cn.ponfee.scheduler.common.base.Symbol.Str.COLON;
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
     * Group name
     *
     * @see SchedJob#getJobGroup()
     */
    private final String group;

    /**
     * Worker id(a string value of UUID)
     */
    private final String workerId;

    /**
     * Serialized value
     */
    private transient final String serializedValue;

    /**
     * Worker supports multiple group
     */
    private transient final Set<String> groups;

    public Worker(String group, String workerId, String host, int port) {
        super(host, port);

        Assert.isTrue(!workerId.contains(COLON), "Worker id cannot contains symbol ':'");
        Assert.isTrue(!group.contains(COLON), "Group name cannot contains symbol ':'");
        this.group = group;
        this.workerId = workerId;

        this.serializedValue = group + COLON + workerId + COLON + host + COLON + port;
        this.groups = split(group, JobConstants.WORKER_GROUP_SEPARATOR);
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
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

    public String getGroup() {
        return group;
    }

    public String getWorkerId() {
        return workerId;
    }

    public List<Worker> splitGroup() {
        return (groups.size() == 1)
            ? Collections.singletonList(this)
            : groups.stream().map(e -> new Worker(e, workerId, host, port)).collect(Collectors.toList());
    }

    public boolean matchesGroup(String taskGroup) {
        // Strings.matches(taskGroup, group);
        // new org.springframework.util.AntPathMatcher();
        return groups.contains(taskGroup);
    }

    public boolean equalsGroup(Worker other) {
        return super.equals(other)
            && Objects.equals(workerId, other.workerId)
            && matchesGroup(other.group);
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
            return of(parser.parseObject());
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
            return of(p.readValueAs(Jsons.MAP_NORMAL));
        }
    }

    public static Worker of(Map<String, ?> map) {
        if (map == null) {
            return null;
        }

        String group = MapUtils.getString(map, "group");
        String workerId = MapUtils.getString(map, "workerId");
        String host = MapUtils.getString(map, "host");
        int port = MapUtils.getIntValue(map, "port");
        return new Worker(group, workerId, host, port);
    }

    private static Set<String> split(String str, String separator) {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        Arrays.stream(str.split(separator))
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .distinct()
            .forEach(builder::add);
        return builder.build();
    }

    /**
     * Holder the current worker context.
     */
    private static class Current {
        private static volatile Worker current;

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
