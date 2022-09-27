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

    @Override
    protected boolean verify(ExecuteParam param) {
        if (param.getWorker() == null) {
            throw new IllegalArgumentException("Worker cannot be null.");
        }
        return true;
    }
}
