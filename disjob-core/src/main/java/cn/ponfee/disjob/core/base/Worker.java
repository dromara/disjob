/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.base;

import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.common.util.Numbers;
import cn.ponfee.disjob.core.model.SchedJob;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static cn.ponfee.disjob.common.base.Symbol.Str.COLON;
import static cn.ponfee.disjob.common.util.Collects.get;
import static cn.ponfee.disjob.core.base.JobConstants.WORKER_MULTIPLE_GROUP_SEPARATOR;

/**
 * Worker for execute task(JVM instance)
 *
 * @author Ponfee
 */
// Also instance of use: @JsonCreator + @JsonProperty("group")
@JsonDeserialize(using = Worker.JacksonDeserializer.class)
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
        this.groups = split(group, WORKER_MULTIPLE_GROUP_SEPARATOR);
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }
        Worker other = (Worker) o;
        return super.equals(other)
            && this.group.equals(other.group)
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

    /**
     * 判断当前Worker机器的group是否匹配任务分配的group
     *
     * @param taskGroup the task group
     * @return {@code true} if matched
     */
    public boolean matchesGroup(String taskGroup) {
        return groups.contains(taskGroup);
    }

    /**
     * 判断当前Worker机器的group是否匹配任务分配的worker
     *
     * @param other the task assigned worker
     * @return {@code true} if matched worker
     */
    public boolean matchesWorker(Worker other) {
        return super.equals(other)
            && this.matchesGroup(other.group)
            && this.workerId.equals(other.workerId);
    }

    /**
     * 判断是否同一台机器：worker-id可以不相等(机器重后，Supervisor缓存的仍是旧的Worker，http派发任务时会出现该情况)
     *
     * @param other the other worker
     * @return {@code true} if same worker
     */
    public boolean sameWorker(Worker other) {
        return super.equals(other)
            && this.matchesGroup(other.group);
    }

    public static Worker current() {
        return Current.current;
    }

    // --------------------------------------------------------custom jackson deserialize

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
