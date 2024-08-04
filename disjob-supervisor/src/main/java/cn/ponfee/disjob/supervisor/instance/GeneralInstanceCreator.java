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

import cn.ponfee.disjob.core.dto.worker.SplitJobParam;
import cn.ponfee.disjob.core.enums.RunType;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.supervisor.component.AbstractJobManager;
import lombok.Getter;

import java.util.List;

/**
 * General instance creator
 *
 * @author Ponfee
 */
public class GeneralInstanceCreator extends TriggerInstanceCreator<GeneralInstanceCreator.GeneralInstance> {

    public GeneralInstanceCreator(AbstractJobManager jobManager) {
        super(jobManager);
    }

    @Override
    public GeneralInstance create(SchedJob job, RunType runType, long triggerTime) throws JobException {
        long instanceId = jobManager.generateId();
        SchedInstance instance = SchedInstance.create(instanceId, job.getJobId(), runType, triggerTime, 0);
        List<SchedTask> tasks = jobManager.splitJob(SplitJobParam.from(job, job.getJobExecutor()), instanceId);
        return new GeneralInstance(instance, tasks);
    }

    @Override
    public void dispatch(SchedJob job, GeneralInstance instance) {
        jobManager.dispatch(job, instance.getInstance(), instance.getTasks());
    }

    @Getter
    public static class GeneralInstance extends TriggerInstance {
        private final List<SchedTask> tasks;

        public GeneralInstance(SchedInstance instance, List<SchedTask> tasks) {
            super(instance);
            this.tasks = tasks;
        }
    }

}
