/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Constrain class must be singleton instance.
 *
 * @author Ponfee
 */
public abstract class SingletonClassConstraint {

    private static final Logger LOG = LoggerFactory.getLogger(SingletonClassConstraint.class);
    private static final Set<Class<?>> MUTEX = new HashSet<>();

    protected SingletonClassConstraint() {
        constrain(this);
    }

    public static synchronized void constrain(Object instance) {
        Objects.requireNonNull(instance, "Object instance cannot be null.");
        constrain(instance.getClass());
    }

    public static synchronized void constrain(Class<?> clazz) {
        if (MUTEX.add(clazz)) {
            LOG.info("Class '" + clazz + "' instance are created.");
        } else {
            throw new Error("Class '" + clazz + "' instance already created.");
        }
    }

}
