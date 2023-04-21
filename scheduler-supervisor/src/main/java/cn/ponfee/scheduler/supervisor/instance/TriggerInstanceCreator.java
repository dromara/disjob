/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.instance;

import cn.ponfee.scheduler.core.enums.JobType;
import cn.ponfee.scheduler.core.enums.RunType;
import cn.ponfee.scheduler.core.exception.JobException;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.supervisor.manager.SchedulerJobManager;

/**
 * Trigger instance creator
 *
 * @author Ponfee
 */
public abstract class TriggerInstanceCreator<T extends TriggerInstance> {

    protected final SchedulerJobManager manager;

    public TriggerInstanceCreator(SchedulerJobManager manager) {
        this.manager = manager;
    }

    public final void createAndDispatch(SchedJob job, RunType runType, long triggerTime) throws JobException {
        T ti = create(job, runType, triggerTime);
        if (manager.createInstance(job, ti)) {
            dispatch(job, ti);
        }
    }

    /**
     * Creates instance and tasks
     *
     * @param job         the sched job
     * @param runType     the run type
     * @param triggerTime the trigger time
     * @return TriggerInstance object
     * @throws JobException if split task occur JobException
     */
    public abstract T create(SchedJob job, RunType runType, long triggerTime) throws JobException;

    /**
     * Dispatch created task
     *
     * @param job      the sched job
     * @param instance the instance
     */
    public abstract void dispatch(SchedJob job, T instance);

    public static TriggerInstanceCreator<?> of(Integer jobType, SchedulerJobManager manager) {
        switch (JobType.of(jobType)) {
            case NORMAL:
                return new NormalInstanceCreator(manager);
            case WORKFLOW:
                return new WorkflowInstanceCreator(manager);
            default:
                throw new UnsupportedOperationException("Unknown job type: " + jobType);
        }
    }

}
