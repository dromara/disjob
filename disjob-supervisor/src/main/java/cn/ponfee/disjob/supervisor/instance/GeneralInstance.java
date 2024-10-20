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

import cn.ponfee.disjob.core.enums.RunType;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.supervisor.base.ModelConverter;
import cn.ponfee.disjob.supervisor.component.JobManager;
import cn.ponfee.disjob.supervisor.model.SchedInstance;
import cn.ponfee.disjob.supervisor.model.SchedJob;
import cn.ponfee.disjob.supervisor.model.SchedTask;

import java.util.List;

/**
 * General instance
 *
 * @author Ponfee
 */
public class GeneralInstance extends TriggerInstance {

    private List<SchedTask> tasks;

    protected GeneralInstance(JobManager jobManager, SchedJob job) {
        super(jobManager, job);
    }

    @Override
    protected void create(SchedInstance parent, RunType runType, long triggerTime) throws JobException {
        long instanceId = jobManager.generateId();
        super.instance = SchedInstance.of(parent, null, instanceId, job.getJobId(), runType, triggerTime, 0);
        this.tasks = jobManager.splitJob(ModelConverter.toSplitJobParam(job, instance), instanceId);
    }

    @Override
    public void save() {
        jobManager.saveInstanceAndTasks(instance, tasks);
    }

    @Override
    public void dispatch() {
        jobManager.dispatch(job, instance, tasks);
    }

}
