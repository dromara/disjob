package cn.ponfee.scheduler.registry;

import cn.ponfee.scheduler.common.util.ClassUtils;
import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.Server;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import org.springframework.util.Assert;

import java.lang.reflect.Modifier;

/**
 * Role definition.
 *
 * @author Ponfee
 */
public enum Roles {

    /**
     * Worker
     */
    WORKER(Worker.class, JobConstants.SCHEDULER_KEY_PREFIX + ".workers"),

    /**
     * Supervisor
     */
    SUPERVISOR(Supervisor.class, JobConstants.SCHEDULER_KEY_PREFIX + ".supervisors"),

    ;

    private final Class<? extends Server> serverType;
    private final String registryKey;

    Roles(Class<? extends Server> serverType, String registryKey) {
        Assert.isTrue(!Modifier.isAbstract(serverType.getModifiers()), "Server type cannot be abstract class: " + serverType);
        this.serverType = serverType;
        this.registryKey = registryKey;
    }

    public <T extends Server> Class<T> serverType() {
        return (Class<T>) serverType;
    }

    public String registryKey() {
        return registryKey;
    }

    public <T extends Server> T deserialize(String text) {
        return ClassUtils.invoke(serverType(), "deserialize", new Object[]{text});
    }

    public static Roles of(Class<? extends Server> serverType) {
        for (Roles value : values()) {
            if (serverType == value.serverType) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown server type: " + serverType);
    }

}
