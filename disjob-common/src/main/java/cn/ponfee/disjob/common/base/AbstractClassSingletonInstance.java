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
import java.util.Set;

/**
 * Abstract class singleton instance
 *
 * @author Ponfee
 */
public abstract class AbstractClassSingletonInstance {

    private static final Set<Class<? extends AbstractClassSingletonInstance>> MUTEX = new HashSet<>();

    protected final Logger log = LoggerFactory.getLogger(getClass());

    public AbstractClassSingletonInstance() {
        synchronized (MUTEX) {
            Class<? extends AbstractClassSingletonInstance> clazz = getClass();
            if (MUTEX.contains(clazz)) {
                throw new Error("Class '" + clazz + "' instance already created.");
            }
            log.info("Class '" + clazz + "' instance was created.");
            MUTEX.add(clazz);
        }
    }

}
