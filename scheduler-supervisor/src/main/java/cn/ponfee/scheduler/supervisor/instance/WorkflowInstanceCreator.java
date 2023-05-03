/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.instance;

import cn.ponfee.scheduler.common.graph.DAGEdge;
import cn.ponfee.scheduler.common.graph.DAGExpressionParser;
import cn.ponfee.scheduler.common.graph.DAGNode;
import cn.ponfee.scheduler.common.tuple.Tuple2;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.enums.RunState;
import cn.ponfee.scheduler.core.enums.RunType;
import cn.ponfee.scheduler.core.exception.JobException;
import cn.ponfee.scheduler.core.graph.WorkflowGraph;
import cn.ponfee.scheduler.core.model.*;
import cn.ponfee.scheduler.supervisor.manager.SchedulerJobManager;
import cn.ponfee.scheduler.supervisor.param.SplitJobParam;
import lombok.Getter;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
        long wnstanceId = manager.generateId();
        SchedInstance leadInstance = SchedInstance.create(wnstanceId, job.getJobId(), runType, triggerTime, 0, now);
        leadInstance.setRunState(RunState.RUNNING.value());
        leadInstance.setRunStartTime(now);
        leadInstance.setWnstanceId(wnstanceId);

        MutableInt sequence = new MutableInt(1);
        List<SchedWorkflow> workflows = new DAGExpressionParser(job.getJobHandler())
            .parse()
            .edges()
            .stream()
            .map(e -> new SchedWorkflow(wnstanceId, e.target().toString(), e.source().toString(), sequence.getAndIncrement()))
            .collect(Collectors.toList());

        List<Tuple2<SchedInstance, List<SchedTask>>> nodeInstances = new ArrayList<>();
        for (Map.Entry<DAGEdge, SchedWorkflow> firstTriggers : new WorkflowGraph(workflows).successors(DAGNode.START).entrySet()) {
            DAGNode node = firstTriggers.getKey().getTarget();
            SchedWorkflow workflow = firstTriggers.getValue();

            // 加sequence解决唯一索引问题：UNIQUE KEY `uk_jobid_triggertime_runtype` (`job_id`, `trigger_time`, `run_type`)
            long nodeInstanceId = manager.generateId();
            long nodeTriggerTime = triggerTime + workflow.getSequence();

            workflow.setInstanceId(nodeInstanceId);
            workflow.setRunState(RunState.RUNNING.value());

            // 工作流的子任务实例的【root/parent/workflow】instance_id只与工作流相关联
            SchedInstance nodeInstance = SchedInstance.create(nodeInstanceId, job.getJobId(), runType, nodeTriggerTime, 0, now);
            nodeInstance.setRnstanceId(wnstanceId);
            nodeInstance.setPnstanceId(wnstanceId);
            nodeInstance.setWnstanceId(wnstanceId);
            nodeInstance.setAttach(Jsons.toJson(InstanceAttach.of(node)));

            SplitJobParam param = SplitJobParam.from(job, node.getName());
            List<SchedTask> tasks = manager.splitTasks(param, nodeInstance.getInstanceId(), now);
            nodeInstances.add(Tuple2.of(nodeInstance, tasks));
        }

        return new WorkflowInstance(leadInstance, workflows, nodeInstances);
    }

    @Override
    public void dispatch(SchedJob job, WorkflowInstance wnstance) {
        for (Tuple2<SchedInstance, List<SchedTask>> nodeInstance : wnstance.getNodeInstances()) {
            manager.dispatch(job, nodeInstance.a, nodeInstance.b);
        }
    }

    @Getter
    public static class WorkflowInstance extends TriggerInstance {
        private final List<SchedWorkflow> workflows;
        private final List<Tuple2<SchedInstance, List<SchedTask>>> nodeInstances;

        public WorkflowInstance(SchedInstance instance,
                                List<SchedWorkflow> workflows,
                                List<Tuple2<SchedInstance, List<SchedTask>>> nodeInstances) {
            super(instance);
            this.workflows = workflows;
            this.nodeInstances = nodeInstances;
        }
    }

}
