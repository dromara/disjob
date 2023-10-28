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
import cn.ponfee.disjob.common.util.Strings;
import cn.ponfee.disjob.core.model.SchedJob;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.commons.collections4.MapUtils;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static cn.ponfee.disjob.common.base.Symbol.Str.COLON;
import static cn.ponfee.disjob.common.collect.Collects.get;

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

    public Worker(String group, String workerId, String host, int port) {
        super(host, port);

        Assert.isTrue(!workerId.contains(COLON), "Worker id cannot contains symbol ':'");
        Assert.isTrue(!group.contains(COLON), "Group name cannot contains symbol ':'");
        this.group = Strings.requireNonBlank(group.trim());
        this.workerId = workerId;

        this.serializedValue = group + COLON + workerId + COLON + host + COLON + port;
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

    /**
     * 判断当前Worker的group是否匹配任务分派的Worker的group
     *
     * @param taskGroup the task group
     * @return {@code true} if matched
     */
    public boolean matchesGroup(String taskGroup) {
        return this.group.equals(taskGroup);
    }

    /**
     * 判断是否同一台机器：其它属性相同但worker-id不同
     * Worker机器宕机重启后，Supervisor一段时间内仍缓存着旧的Worker，导致http通过ip:port派发的任务参数中Worker是旧的，但是新Worker接收该任务
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

    public static boolean isCurrent(Worker worker) {
        return worker != null && worker.equals(current());
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
