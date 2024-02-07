/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.base;

import java.io.Closeable;
import java.util.Collections;
import java.util.Set;

/**
 * Representing startable
 *
 * @author Ponfee
 */
public interface Startable extends Closeable {

    /**
     * Returns list of dependencies startable
     *
     * @return dependency other startable
     */
    default Set<Startable> dependencies() {
        return Collections.emptySet();
    }

    /**
     * Start
     */
    void start();

    /**
     * Stop
     */
    void stop();

    /**
     * Close
     */
    @Override
    default void close() {
        stop();
    }

}
