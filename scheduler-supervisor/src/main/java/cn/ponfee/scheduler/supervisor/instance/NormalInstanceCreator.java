/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.instance;

import cn.ponfee.scheduler.core.enums.RunType;
import cn.ponfee.scheduler.core.exception.JobException;
import cn.ponfee.scheduler.core.model.SchedInstance;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.supervisor.manager.SchedulerJobManager;
import cn.ponfee.scheduler.supervisor.param.SplitJobParam;
import lombok.Getter;

import java.util.Date;
import java.util.List;

/**
 * Normal instance creator
 *
 * @author Ponfee
 */
public class NormalInstanceCreator extends TriggerInstanceCreator<NormalInstanceCreator.NormalInstance> {

    public NormalInstanceCreator(SchedulerJobManager manager) {
        super(manager);
    }

    @Override
    public NormalInstance create(SchedJob job, RunType runType, long triggerTime) throws JobException {
        Date now = new Date();
        long instanceId = manager.generateId();
        SchedInstance instance = SchedInstance.create(instanceId, job.getJobId(), runType, triggerTime, 0, now);
        List<SchedTask> tasks = manager.splitTasks(SplitJobParam.from(job), instanceId, now);
        return new NormalInstance(instance, tasks);
    }

    @Override
    public void dispatch(SchedJob job, NormalInstance instance) {
        manager.dispatch(job, instance.getInstance(), instance.getTasks());
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
