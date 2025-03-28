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

import cn.ponfee.disjob.common.base.TriConsumer;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.RunType;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.supervisor.component.JobManager;
import cn.ponfee.disjob.supervisor.model.SchedInstance;
import cn.ponfee.disjob.supervisor.model.SchedJob;
import cn.ponfee.disjob.supervisor.model.SchedTask;
import cn.ponfee.disjob.supervisor.model.SchedWorkflow;

import java.util.List;

/**
 * Abstract trigger instance
 *
 * @author Ponfee
 */
public abstract class TriggerInstance {

    protected final JobManager jobManager;
    protected final SchedJob job;

    protected SchedInstance instance;

    protected TriggerInstance(JobManager jobManager, SchedJob job) {
        this.jobManager = jobManager;
        this.job = job;
    }

    /**
     * Creates instance and tasks
     *
     * @param parent      the parent instance
     * @param runType     the run type
     * @param triggerTime the trigger time
     * @throws JobException if split task occur JobException
     */
    abstract void create(SchedInstance parent, RunType runType, long triggerTime) throws JobException;

    public abstract void save(TriConsumer<List<SchedInstance>, List<SchedWorkflow>, List<SchedTask>> persistence);

    public abstract void dispatch(TriConsumer<SchedJob, SchedInstance, List<SchedTask>> dispatching);

    public static TriggerInstance of(JobManager jobManager, SchedJob job,
                                     SchedInstance parent, RunType runType, long triggerTime) throws JobException {
        JobType jobType = JobType.of(job.getJobType());
        TriggerInstance triggerInstance;
        switch (jobType) {
            case GENERAL:
                triggerInstance = new GeneralInstance(jobManager, job);
                break;
            case WORKFLOW:
                triggerInstance = new WorkflowInstance(jobManager, job);
                break;
            default:
                throw new UnsupportedOperationException("Unknown job type: " + jobType);
        }
        triggerInstance.create(parent, runType, triggerTime);

        return triggerInstance;
    }

}
