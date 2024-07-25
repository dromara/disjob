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

import cn.ponfee.disjob.common.dag.DAGEdge;
import cn.ponfee.disjob.common.dag.DAGExpressionParser;
import cn.ponfee.disjob.common.dag.DAGNode;
import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.tuple.Tuple2;
import cn.ponfee.disjob.core.dto.worker.SplitJobParam;
import cn.ponfee.disjob.core.enums.RunState;
import cn.ponfee.disjob.core.enums.RunType;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.model.*;
import cn.ponfee.disjob.supervisor.component.DistributedJobManager;
import cn.ponfee.disjob.supervisor.dag.WorkflowGraph;
import lombok.Getter;

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

    public WorkflowInstanceCreator(DistributedJobManager jobManager) {
        super(jobManager);
    }

    @Override
    public WorkflowInstance create(SchedJob job, RunType runType, long triggerTime) throws JobException {
        long wnstanceId = jobManager.generateId();
        SchedInstance leadInstance = SchedInstance.create(wnstanceId, job.getJobId(), runType, triggerTime, 0);
        leadInstance.setRunState(RunState.RUNNING.value());
        leadInstance.setRunStartTime(Dates.max(new Date(), new Date(triggerTime)));
        leadInstance.setWnstanceId(wnstanceId);

        List<SchedWorkflow> workflows = DAGExpressionParser.parse(job.getJobHandler())
            .edges()
            .stream()
            .map(e -> new SchedWorkflow(wnstanceId, e.source().toString(), e.target().toString()))
            .collect(Collectors.toList());

        List<Tuple2<SchedInstance, List<SchedTask>>> nodeInstances = new ArrayList<>();
        for (Map.Entry<DAGEdge, SchedWorkflow> firstTriggers : new WorkflowGraph(workflows).successors(DAGNode.START).entrySet()) {
            DAGNode node = firstTriggers.getKey().getTarget();
            SchedWorkflow workflow = firstTriggers.getValue();

            long nodeInstanceId = jobManager.generateId();
            workflow.setInstanceId(nodeInstanceId);
            workflow.setRunState(RunState.RUNNING.value());

            // 工作流的子任务实例的【root、parent、workflow】instance_id只与工作流相关联
            SchedInstance nodeInstance = SchedInstance.create(nodeInstanceId, job.getJobId(), runType, triggerTime, 0);
            nodeInstance.setRnstanceId(wnstanceId);
            nodeInstance.setPnstanceId(wnstanceId);
            nodeInstance.setWnstanceId(wnstanceId);
            nodeInstance.setAttach(new InstanceAttach(node.toString()).toJson());

            SplitJobParam param = SplitJobParam.from(job, node.getName());
            List<SchedTask> tasks = jobManager.splitJob(param, nodeInstance.getInstanceId());
            nodeInstances.add(Tuple2.of(nodeInstance, tasks));
        }

        return new WorkflowInstance(leadInstance, workflows, nodeInstances);
    }

    @Override
    public void dispatch(SchedJob job, WorkflowInstance wnstance) {
        for (Tuple2<SchedInstance, List<SchedTask>> nodeInstance : wnstance.getNodeInstances()) {
            jobManager.dispatch(job, nodeInstance.a, nodeInstance.b);
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
