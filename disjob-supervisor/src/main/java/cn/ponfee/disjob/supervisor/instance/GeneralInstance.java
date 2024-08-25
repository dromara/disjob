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
import cn.ponfee.disjob.core.dto.worker.SplitJobParam;
import cn.ponfee.disjob.core.enums.RunType;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.core.model.SchedTask;

import java.util.List;

import static cn.ponfee.disjob.core.base.JobConstants.PROCESS_BATCH_SIZE;

/**
 * General instance
 *
 * @author Ponfee
 */
public class GeneralInstance extends TriggerInstance {

    private List<SchedTask> tasks;

    public GeneralInstance(Creator creator, SchedJob job) {
        super(creator, job);
    }

    @Override
    protected void create(SchedInstance parent, RunType runType, long triggerTime) throws JobException {
        long instanceId = c.jobManager.generateId();
        super.instance = SchedInstance.create(parent, null, instanceId, job.getJobId(), runType, triggerTime, 0);
        this.tasks = c.jobManager.splitJob(SplitJobParam.from(job, job.getJobExecutor()), instanceId);
    }

    @Override
    public void save() {
        c.instanceMapper.insert(instance.fillUniqueFlag());
        Collects.batchProcess(tasks, c.taskMapper::batchInsert, PROCESS_BATCH_SIZE);
    }

    @Override
    public void dispatch() {
        c.jobManager.dispatch(job, instance, tasks);
    }

}
