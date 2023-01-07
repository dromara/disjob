/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.handle;

/**
 * Save task execution snapshot
 *
 * @author Ponfee
 */
@FunctionalInterface
public interface Checkpoint {

    /**
     * Save the task execution snapshot
     *
     * @param taskId         the task id
     * @param executeSnapshot the execution snapshot data
     * @return {@code true} if saved successfully
     * @throws Exception if saved occur exception
     */
    boolean checkpoint(long taskId, String executeSnapshot) throws Exception;

    Checkpoint DISCARD = (taskId, executeSnapshot) -> true;
}
