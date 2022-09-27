package cn.ponfee.scheduler.core.base;

import cn.ponfee.scheduler.common.util.Numbers;
import org.springframework.util.Assert;

import java.util.Objects;

import static cn.ponfee.scheduler.common.base.Constants.SEMICOLON;
import static cn.ponfee.scheduler.common.util.Collects.get;


/**
 * Supervisor(JVM instance)
 *
 * @author Ponfee
 */
public class Supervisor extends Server {
    private static final long serialVersionUID = -1254559108807415145L;

    private transient final String serialize;

    public Supervisor(String host, int port) {
        super(host, port);
        this.serialize = host + SEMICOLON + port;
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public String serialize() {
        return serialize;
    }

    /**
     * Deserialize from string.
     *
     * @param text the serialized text string
     * @return supervisor object of the text deserialized result
     */
    public static Supervisor deserialize(String text) {
        Assert.hasText(text, "Serialized text cannot empty.");
        String[] array = text.split(SEMICOLON);

        String host = get(array, 0);
        Assert.hasText(host, "Supervisor host cannot bank.");

        int port = Numbers.toInt(get(array, 1));

        return new Supervisor(host, port);
    }

    // --------------------------------------------------------Current supervisor

    public static Supervisor current() {
        return Current.current;
    }

    private static class Current {
        private static Supervisor current;


        private static synchronized void set(Supervisor supervisor) {
            if (supervisor == null) {
                throw new AssertionError("Current supervisor cannot set null.");
            }
            if (current != null) {
                throw new AssertionError("Current supervisor already set.");
            }
            current = supervisor;
        }
    }

}
