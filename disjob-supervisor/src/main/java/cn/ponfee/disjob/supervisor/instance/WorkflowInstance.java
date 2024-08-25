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
import cn.ponfee.disjob.supervisor.dag.WorkflowGraph;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.ponfee.disjob.core.base.JobConstants.PROCESS_BATCH_SIZE;

/**
 * Workflow instance
 *
 * @author Ponfee
 */
public class WorkflowInstance extends TriggerInstance {

    private List<SchedWorkflow> workflows;
    private List<Tuple2<SchedInstance, List<SchedTask>>> nodeInstances;

    public WorkflowInstance(Creator creator, SchedJob job) {
        super(creator, job);
    }

    @Override
    protected void create(SchedInstance parent, RunType runType, long triggerTime) throws JobException {
        long wnstanceId = c.jobManager.generateId();
        long jobId = job.getJobId();
        SchedInstance leadInstance = SchedInstance.create(parent, wnstanceId, wnstanceId, jobId, runType, triggerTime, 0);
        leadInstance.setRunState(RunState.RUNNING.value());
        leadInstance.setRunStartTime(Dates.max(new Date(), new Date(triggerTime)));
        super.instance = leadInstance;

        this.workflows = DAGExpressionParser.parse(job.getJobExecutor())
            .edges()
            .stream()
            .map(e -> new SchedWorkflow(wnstanceId, e.source().toString(), e.target().toString()))
            .collect(Collectors.toList());

        this.nodeInstances = new ArrayList<>();
        for (Map.Entry<DAGEdge, SchedWorkflow> first : WorkflowGraph.of(workflows).successors(DAGNode.START).entrySet()) {
            DAGNode node = first.getKey().getTarget();
            SchedWorkflow workflow = first.getValue();

            long nodeInstanceId = c.jobManager.generateId();
            workflow.setInstanceId(nodeInstanceId);
            workflow.setRunState(RunState.RUNNING.value());

            // 工作流的子任务实例的【root、parent、workflow】instance_id只与工作流相关联
            SchedInstance nodeInstance = SchedInstance.create(leadInstance, nodeInstanceId, jobId, runType, triggerTime, 0);
            nodeInstance.setAttach(new InstanceAttach(node.toString()).toJson());

            SplitJobParam param = SplitJobParam.from(job, node.getName());
            List<SchedTask> tasks = c.jobManager.splitJob(param, nodeInstance.getInstanceId());
            nodeInstances.add(Tuple2.of(nodeInstance, tasks));
        }
    }

    @Override
    public void save() {
        c.instanceMapper.insert(instance.fillUniqueFlag());
        Collects.batchProcess(workflows, c.workflowMapper::batchInsert, PROCESS_BATCH_SIZE);
        for (Tuple2<SchedInstance, List<SchedTask>> nodeInstance : nodeInstances) {
            c.instanceMapper.insert(nodeInstance.a.fillUniqueFlag());
            Collects.batchProcess(nodeInstance.b, c.taskMapper::batchInsert, PROCESS_BATCH_SIZE);
        }
    }

    @Override
    public void dispatch() {
        for (Tuple2<SchedInstance, List<SchedTask>> nodeInstance : nodeInstances) {
            c.jobManager.dispatch(job, nodeInstance.a, nodeInstance.b);
        }
    }

}
