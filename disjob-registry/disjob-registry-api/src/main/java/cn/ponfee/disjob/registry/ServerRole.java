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

package cn.ponfee.disjob.registry;

import cn.ponfee.disjob.common.util.ClassUtils;
import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.Worker;
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
    WORKER(Worker.class, JobConstants.DISJOB_KEY_PREFIX + ".worker"),

    /**
     * Supervisor
     */
    SUPERVISOR(Supervisor.class, JobConstants.DISJOB_KEY_PREFIX + ".supervisor"),

    ;

    private final Class<? extends Server> type;
    private final String key;

    ServerRole(Class<? extends Server> type, String key) {
        Assert.isTrue(!Modifier.isAbstract(type.getModifiers()), () -> "Server type cannot be abstract class: " + type);
        this.type = type;
        this.key = key;
    }

    @SuppressWarnings("unchecked")
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
