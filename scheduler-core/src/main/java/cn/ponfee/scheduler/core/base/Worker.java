package cn.ponfee.scheduler.core.base;

import cn.ponfee.scheduler.common.base.Constants;
import cn.ponfee.scheduler.common.util.Numbers;
import org.springframework.util.Assert;

import java.util.Objects;

import static cn.ponfee.scheduler.common.base.Constants.SEMICOLON;
import static cn.ponfee.scheduler.common.util.Collects.get;

/**
 * Worker(JVM instance)
 *
 * @author Ponfee
 */
public class Worker extends Server {
    private static final long serialVersionUID = 8981019172872301692L;

    /**
     * Group name, @see SchedJob#getJobGroup()
     */
    protected final String group;

    /**
     * Instance id(Auto generate when class {@code Constants} loaded)
     */
    protected final String instanceId;

    private transient final String serialize;

    public Worker(String group, String instanceId, String host, int port) {
        super(host, port);

        Assert.isTrue(!instanceId.contains(Constants.SEMICOLON), "Instance-id cannot contains symbol ':'");
        Assert.isTrue(!group.contains(Constants.SEMICOLON), "Group name cannot contains symbol ':'");
        this.group = group;
        this.instanceId = instanceId;

        this.serialize = group + SEMICOLON + instanceId + SEMICOLON + host + SEMICOLON + port;
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
        return serialize;
    }

    /**
     * Deserialize from string.
     *
     * @param text the serialized text string
     * @return worker object of the text deserialized result
     */
    public static Worker deserialize(String text) {
        Assert.hasText(text, "Serialized text cannot empty.");
        String[] array = text.split(SEMICOLON);

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
