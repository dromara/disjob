/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.event;

import lombok.Getter;

/**
 * Task dispatch failed event
 *
 * @author Ponfee
 */
@Getter
public class TaskDispatchFailedEvent {

    private final long jobId;
    private final long instanceId;
    private final long taskId;

    public TaskDispatchFailedEvent(long jobId, long instanceId, long taskId) {
        this.jobId = jobId;
        this.instanceId = instanceId;
        this.taskId = taskId;
    }

}
