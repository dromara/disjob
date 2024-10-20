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
import cn.ponfee.disjob.common.dag.DAGExpression;
import cn.ponfee.disjob.common.dag.DAGNode;
import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.tuple.Tuple2;
import cn.ponfee.disjob.core.dto.worker.SplitJobParam;
import cn.ponfee.disjob.core.enums.RunState;
import cn.ponfee.disjob.core.enums.RunType;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.supervisor.base.ModelConverter;
import cn.ponfee.disjob.supervisor.component.JobManager;
import cn.ponfee.disjob.supervisor.dag.WorkflowGraph;
import cn.ponfee.disjob.supervisor.model.SchedInstance;
import cn.ponfee.disjob.supervisor.model.SchedJob;
import cn.ponfee.disjob.supervisor.model.SchedTask;
import cn.ponfee.disjob.supervisor.model.SchedWorkflow;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Workflow instance
 *
 * @author Ponfee
 */
public class WorkflowInstance extends TriggerInstance {

    private List<SchedWorkflow> workflows;
    private List<Tuple2<SchedInstance, List<SchedTask>>> nodes;

    protected WorkflowInstance(JobManager jobManager, SchedJob job) {
        super(jobManager, job);
    }

    @Override
    protected void create(SchedInstance parent, RunType runType, long triggerTime) throws JobException {
        long wnstanceId = jobManager.generateId();
        long jobId = job.getJobId();
        SchedInstance leadInstance = SchedInstance.of(parent, wnstanceId, wnstanceId, jobId, runType, triggerTime, 0);
        leadInstance.setRunState(RunState.RUNNING.value());
        leadInstance.setRunStartTime(Dates.max(new Date(), new Date(triggerTime)));
        super.instance = leadInstance;

        this.workflows = DAGExpression.parse(job.getJobExecutor())
            .edges()
            .stream()
            .map(e -> SchedWorkflow.of(wnstanceId, e.source().toString(), e.target().toString()))
            .collect(Collectors.toList());

        // 生成第一批待执行的工作流实例
        this.nodes = new ArrayList<>();
        for (Map.Entry<DAGEdge, SchedWorkflow> each : WorkflowGraph.of(workflows).successors(DAGNode.START).entrySet()) {
            DAGNode node = each.getKey().getTarget();
            SchedWorkflow workflow = each.getValue();

            long nodeInstanceId = jobManager.generateId();
            workflow.setInstanceId(nodeInstanceId);
            workflow.setRunState(RunState.RUNNING.value());

            // 工作流的子任务实例的【root、parent、workflow】instance_id只与工作流相关联
            SchedInstance nodeInstance = SchedInstance.of(leadInstance, nodeInstanceId, jobId, runType, triggerTime, 0);
            nodeInstance.setWorkflowCurNode(node.toString());

            SplitJobParam splitJobParam = ModelConverter.toSplitJobParam(job, nodeInstance, null);
            List<SchedTask> nodeTasks = jobManager.splitJob(splitJobParam, nodeInstance.getInstanceId());
            nodes.add(Tuple2.of(nodeInstance, nodeTasks));
        }
    }

    @Override
    public void save() {
        // save lead instance & workflow graph
        jobManager.saveInstanceAndWorkflows(instance, workflows);
        for (Tuple2<SchedInstance, List<SchedTask>> node : nodes) {
            // save node instance and node tasks
            jobManager.saveInstanceAndTasks(node.a, node.b);
        }
    }

    @Override
    public void dispatch() {
        for (Tuple2<SchedInstance, List<SchedTask>> node : nodes) {
            jobManager.dispatch(job, node.a, node.b);
        }
    }

}
