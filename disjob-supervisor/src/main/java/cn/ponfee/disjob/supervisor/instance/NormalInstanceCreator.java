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
import cn.ponfee.disjob.core.param.JobHandlerParam;
import cn.ponfee.disjob.supervisor.manager.DistributedJobManager;
import lombok.Getter;

import java.util.Date;
import java.util.List;

/**
 * Normal instance creator
 *
 * @author Ponfee
 */
public class NormalInstanceCreator extends TriggerInstanceCreator<NormalInstanceCreator.NormalInstance> {

    public NormalInstanceCreator(DistributedJobManager jobManager) {
        super(jobManager);
    }

    @Override
    public NormalInstance create(SchedJob job, RunType runType, long triggerTime) throws JobException {
        Date now = new Date();
        long instanceId = jobManager.generateId();
        SchedInstance instance = SchedInstance.create(instanceId, job.getJobId(), runType, triggerTime, 0, now);
        List<SchedTask> tasks = jobManager.splitTasks(JobHandlerParam.from(job), instanceId, now);
        return new NormalInstance(instance, tasks);
    }

    @Override
    public void dispatch(SchedJob job, NormalInstance instance) {
        jobManager.dispatch(job, instance.getInstance(), instance.getTasks());
    }

    @Getter
    public static class NormalInstance extends TriggerInstance {
        private final List<SchedTask> tasks;

        public NormalInstance(SchedInstance instance, List<SchedTask> tasks) {
            super(instance);
            this.tasks = tasks;
        }
    }

}
