/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.worker.base;

import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;

import java.util.Objects;

/**
 * Timing wheel for execute sched task.
 *
 * @author Ponfee
 */
public class TaskTimingWheel extends TimingWheel<ExecuteTaskParam> {
    private static final long serialVersionUID = 5234431161365689615L;

    public TaskTimingWheel(long tickMs, int ringSize) {
        super(tickMs, ringSize);
    }

    @Override
    protected boolean verify(ExecuteTaskParam param) {
        Objects.requireNonNull(param, "Execute task param cannot be null.");
        Objects.requireNonNull(param.getWorker(), "Execute task worker cannot be null.");
        return true;
    }

}
