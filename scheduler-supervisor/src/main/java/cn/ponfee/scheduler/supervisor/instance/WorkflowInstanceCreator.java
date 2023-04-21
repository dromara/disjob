/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.instance;

import cn.ponfee.scheduler.common.base.tuple.Tuple2;
import cn.ponfee.scheduler.common.graph.DAGEdge;
import cn.ponfee.scheduler.common.graph.DAGExpressionParser;
import cn.ponfee.scheduler.common.graph.DAGNode;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.enums.RunState;
import cn.ponfee.scheduler.core.enums.RunType;
import cn.ponfee.scheduler.core.exception.JobException;
import cn.ponfee.scheduler.core.graph.WorkflowGraph;
import cn.ponfee.scheduler.core.model.*;
import cn.ponfee.scheduler.supervisor.manager.SchedulerJobManager;
import cn.ponfee.scheduler.supervisor.param.SplitJobParam;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Workflow instance creator
 *
 * @author Ponfee
 */
public class WorkflowInstanceCreator extends TriggerInstanceCreator<WorkflowInstanceCreator.WorkflowInstance> {

    public WorkflowInstanceCreator(SchedulerJobManager manager) {
        super(manager);
    }

    @Override
    public WorkflowInstance create(SchedJob job, RunType runType, long triggerTime) throws JobException {
        Date now = new Date();
        long instanceId = manager.generateId();
        SchedInstance instance = SchedInstance.create(instanceId, job.getJobId(), runType, triggerTime, 0, now);
        instance.setRunState(RunState.RUNNING.value());
        instance.setRunStartTime(now);
        instance.setWorkflowInstanceId(instanceId);

        AtomicInteger sequence = new AtomicInteger(1);
        List<SchedWorkflow> workflows = new DAGExpressionParser(job.getJobHandler())
            .parse()
            .edges()
            .stream()
            .map(e -> new SchedWorkflow(instanceId, e.target().toString(), e.source().toString(), sequence.getAndIncrement()))
            .collect(Collectors.toList());

        List<Tuple2<SchedInstance, List<SchedTask>>> subInstances = new ArrayList<>();
        for (Map.Entry<DAGEdge, SchedWorkflow> each : new WorkflowGraph(workflows).successors(DAGNode.START).entrySet()) {
            // 解决唯一索引问题：UNIQUE KEY `uk_jobid_triggertime_runtype` (`job_id`, `trigger_time`, `run_type`)
            long subTriggerTime = triggerTime + each.getValue().getSequence();
            SchedInstance subInstance = SchedInstance.create(manager.generateId(), job.getJobId(), runType, subTriggerTime, 0, now);
            subInstance.setRootInstanceId(instanceId);
            subInstance.setParentInstanceId(instanceId);
            subInstance.setWorkflowInstanceId(instanceId);
            subInstance.setAttach(Jsons.toJson(WorkflowAttach.of(each.getKey().getTarget())));
            SplitJobParam param = SplitJobParam.from(job, each.getKey().getTarget().getName());
            List<SchedTask> tasks = manager.splitTasks(param, subInstance.getInstanceId(), now);
            subInstances.add(Tuple2.of(subInstance, tasks));
        }

        return new WorkflowInstance(instance, workflows, subInstances);
    }

    @Override
    public void dispatch(SchedJob job, WorkflowInstance instance) {
        for (Tuple2<SchedInstance, List<SchedTask>> subInstance : instance.getSubInstances()) {
            manager.dispatch(job, subInstance.a, subInstance.b);
        }
    }

    @Getter
    public static class WorkflowInstance extends TriggerInstance {
        private final List<SchedWorkflow> workflows;
        private final List<Tuple2<SchedInstance, List<SchedTask>>> subInstances;

        public WorkflowInstance(SchedInstance instance,
                                List<SchedWorkflow> workflows,
                                List<Tuple2<SchedInstance, List<SchedTask>>> subInstances) {
            super(instance);
            this.workflows = workflows;
            this.subInstances = subInstances;
        }
    }

}
