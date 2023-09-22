/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.instance;

import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.RunType;
import cn.ponfee.disjob.core.exception.JobCheckedException;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.supervisor.service.DistributedJobManager;

/**
 * Trigger instance creator
 *
 * @author Ponfee
 */
public abstract class TriggerInstanceCreator<T extends TriggerInstance> {

    protected final DistributedJobManager jobManager;

    public TriggerInstanceCreator(DistributedJobManager jobManager) {
        this.jobManager = jobManager;
    }

    public final void createWithSaveAndDispatch(SchedJob job, RunType runType, long triggerTime) throws JobCheckedException {
        T triggerInstance = create(job, runType, triggerTime);
        if (jobManager.createInstance(job, triggerInstance)) {
            dispatch(job, triggerInstance);
        }
    }

    /**
     * Creates instance and tasks
     *
     * @param job         the sched job
     * @param runType     the run type
     * @param triggerTime the trigger time
     * @return TriggerInstance object
     * @throws JobCheckedException if split task occur JobException
     */
    public abstract T create(SchedJob job, RunType runType, long triggerTime) throws JobCheckedException;

    /**
     * Dispatch created task
     *
     * @param job      the sched job
     * @param instance the instance
     */
    public abstract void dispatch(SchedJob job, T instance);

    public static TriggerInstanceCreator<?> of(Integer jobType, DistributedJobManager jobManager) {
        switch (JobType.of(jobType)) {
            case NORMAL:
                return new NormalInstanceCreator(jobManager);
            case WORKFLOW:
                return new WorkflowInstanceCreator(jobManager);
            default:
                throw new UnsupportedOperationException("Unknown job type: " + jobType);
        }
    }

}
