/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.handle;

import cn.ponfee.disjob.core.exception.JobCheckedException;

import java.util.List;

/**
 * Broadcast job handler
 *
 * @author Ponfee
 */
public abstract class BroadcastJobHandler extends JobHandler {

    @Override
    public final List<SplitTask> split(String jobParam) throws JobCheckedException {
        throw new UnsupportedOperationException("Broadcast job handler unsupported split operation.");
    }

}
