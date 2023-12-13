/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.instance;

import cn.ponfee.disjob.core.enums.RunType;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.core.param.worker.JobHandlerParam;
import cn.ponfee.disjob.supervisor.component.DistributedJobManager;
import lombok.Getter;

import java.util.Date;
import java.util.List;

/**
 * General instance creator
 *
 * @author Ponfee
 */
public class GeneralInstanceCreator extends TriggerInstanceCreator<GeneralInstanceCreator.GeneralInstance> {

    public GeneralInstanceCreator(DistributedJobManager jobManager) {
        super(jobManager);
    }

    @Override
    public GeneralInstance create(SchedJob job, RunType runType, long triggerTime) throws JobException {
        Date now = new Date();
        long instanceId = jobManager.generateId();
        SchedInstance instance = SchedInstance.create(instanceId, job.getJobId(), runType, triggerTime, 0, now);
        List<SchedTask> tasks = jobManager.splitTasks(JobHandlerParam.from(job), instanceId, now);
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
