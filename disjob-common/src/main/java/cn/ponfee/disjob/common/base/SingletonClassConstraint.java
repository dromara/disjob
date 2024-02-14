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

package cn.ponfee.disjob.common.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Constrain class must be singleton instance.
 *
 * @author Ponfee
 */
public abstract class SingletonClassConstraint {

    private static final Logger LOG = LoggerFactory.getLogger(SingletonClassConstraint.class);
    private static final Set<Class<?>> MUTEX = ConcurrentHashMap.newKeySet();

    protected SingletonClassConstraint() {
        constrain(this);
    }

    public static synchronized void constrain(Object instance) {
        Objects.requireNonNull(instance, "Object instance cannot be null.");
        constrain(instance.getClass());
    }

    public static synchronized void constrain(Class<?> clazz) {
        if (MUTEX.add(clazz)) {
            LOG.info("Class '{}' instance are created.", clazz);
        } else {
            throw new Error("Class '" + clazz + "' instance already created.");
        }
    }

}
