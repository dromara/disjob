/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.supervisor.instance;

import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.RunType;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.supervisor.component.DistributedJobManager;

/**
 * Trigger instance creator
 *
 * @author Ponfee
 */
public abstract class TriggerInstanceCreator<T extends TriggerInstance> {

    protected final DistributedJobManager jobManager;

    protected TriggerInstanceCreator(DistributedJobManager jobManager) {
        this.jobManager = jobManager;
    }

    public final void createWithSaveAndDispatch(SchedJob job, RunType runType, long triggerTime) throws JobException {
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

    public static TriggerInstanceCreator<?> of(Integer jobType, DistributedJobManager jobManager) {
        switch (JobType.of(jobType)) {
            case GENERAL:
                return new GeneralInstanceCreator(jobManager);
            case WORKFLOW:
                return new WorkflowInstanceCreator(jobManager);
            default:
                throw new UnsupportedOperationException("Unknown job type: " + jobType);
        }
    }

}
