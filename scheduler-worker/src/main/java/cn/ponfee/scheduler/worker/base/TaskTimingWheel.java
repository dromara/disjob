/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.worker.base;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.core.param.ExecuteParam;

/**
 * Timing wheel for execute sched task.
 *
 * @author Ponfee
 */
public class TaskTimingWheel extends TimingWheel<ExecuteParam> {
    private static final long serialVersionUID = 5234431161365689615L;

    public TaskTimingWheel(long tickMs, int ringSize) {
        super(tickMs, ringSize);
    }

    @Override
    protected boolean verify(ExecuteParam param) {
        if (param.getWorker() == null) {
            throw new IllegalArgumentException("Worker cannot be null.");
        }
        return true;
    }
}
