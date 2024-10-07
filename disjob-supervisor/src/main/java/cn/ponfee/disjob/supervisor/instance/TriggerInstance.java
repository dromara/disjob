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

import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.RunType;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.supervisor.component.AbstractJobManager;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedInstanceMapper;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedTaskMapper;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedWorkflowMapper;
import cn.ponfee.disjob.supervisor.model.SchedInstance;
import cn.ponfee.disjob.supervisor.model.SchedJob;
import cn.ponfee.disjob.supervisor.model.SchedTask;
import cn.ponfee.disjob.supervisor.model.SchedWorkflow;

import java.util.List;

import static cn.ponfee.disjob.core.base.JobConstants.PROCESS_BATCH_SIZE;

/**
 * Abstract trigger instance
 *
 * @author Ponfee
 */
public abstract class TriggerInstance {

    protected final Creator creator;
    protected final SchedJob job;

    protected SchedInstance instance;

    protected TriggerInstance(Creator creator, SchedJob job) {
        this.creator = creator;
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
    protected abstract void create(SchedInstance parent, RunType runType, long triggerTime) throws JobException;

    public abstract void save();

    public abstract void dispatch();

    public static class Creator {
        final AbstractJobManager jobManager;
        private final SchedWorkflowMapper workflowMapper;
        private final SchedInstanceMapper instanceMapper;
        private final SchedTaskMapper taskMapper;

        public Creator(AbstractJobManager jobManager, SchedWorkflowMapper workflowMapper,
                       SchedInstanceMapper instanceMapper, SchedTaskMapper taskMapper) {
            this.jobManager = jobManager;
            this.workflowMapper = workflowMapper;
            this.instanceMapper = instanceMapper;
            this.taskMapper = taskMapper;
        }

        public TriggerInstance create(SchedJob job, SchedInstance parent, RunType runType, long triggerTime) throws JobException {
            JobType jobType = JobType.of(job.getJobType());
            TriggerInstance triggerInstance;
            if (jobType == JobType.GENERAL) {
                triggerInstance = new GeneralInstance(this, job);
            } else if (jobType == JobType.WORKFLOW) {
                triggerInstance = new WorkflowInstance(this, job);
            } else {
                throw new UnsupportedOperationException("Unknown job type: " + jobType);
            }
            triggerInstance.create(parent, runType, triggerTime);

            return triggerInstance;
        }

        public void saveInstanceAndTasks(SchedInstance instance, List<SchedTask> tasks) {
            saveInstance(instance);
            Collects.batchProcess(tasks, taskMapper::batchInsert, PROCESS_BATCH_SIZE);
        }

        void saveWorkflows(List<SchedWorkflow> workflows) {
            Collects.batchProcess(workflows, workflowMapper::batchInsert, PROCESS_BATCH_SIZE);
        }

        void saveInstance(SchedInstance instance) {
            instanceMapper.insert(instance.fillUniqueFlag());
        }
    }

}
