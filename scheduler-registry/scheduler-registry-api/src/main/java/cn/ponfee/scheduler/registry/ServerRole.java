/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.registry;

import cn.ponfee.scheduler.common.util.ClassUtils;
import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.Server;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import org.springframework.util.Assert;

import java.lang.reflect.Modifier;

/**
 * Server role definition.
 *
 * @author Ponfee
 */
public enum ServerRole {

    /**
     * Worker
     */
    WORKER(Worker.class, JobConstants.SCHEDULER_KEY_PREFIX + ".workers"),

    /**
     * Supervisor
     */
    SUPERVISOR(Supervisor.class, JobConstants.SCHEDULER_KEY_PREFIX + ".supervisors"),

    ;

    private final Class<? extends Server> type;
    private final String key;

    ServerRole(Class<? extends Server> type, String key) {
        Assert.isTrue(!Modifier.isAbstract(type.getModifiers()), () -> "Server type cannot be abstract class: " + type);
        this.type = type;
        this.key = key;
    }

    public <T extends Server> Class<T> type() {
        return (Class<T>) type;
    }

    public String key() {
        return key;
    }

    public <T extends Server> T deserialize(String text) {
        return ClassUtils.invoke(type(), "deserialize", new Object[]{text});
    }

    public static ServerRole of(Class<? extends Server> type) {
        for (ServerRole value : values()) {
            if (type == value.type) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown server type: " + type);
    }

}
