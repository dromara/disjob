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

package cn.ponfee.disjob.supervisor.component;

import cn.ponfee.disjob.common.dag.DAGEdge;
import cn.ponfee.disjob.common.dag.DAGNode;
import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.model.PageResponse;
import cn.ponfee.disjob.common.util.Numbers;
import cn.ponfee.disjob.core.enums.RunState;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.core.model.SchedWorkflow;
import cn.ponfee.disjob.core.param.supervisor.WorkflowPredecessorNodeParam;
import cn.ponfee.disjob.supervisor.application.converter.SchedJobConverter;
import cn.ponfee.disjob.supervisor.application.request.SchedInstancePageRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedJobPageRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedJobSearchRequest;
import cn.ponfee.disjob.supervisor.application.response.SchedInstanceResponse;
import cn.ponfee.disjob.supervisor.application.response.SchedJobResponse;
import cn.ponfee.disjob.supervisor.dag.WorkflowGraph;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedInstanceMapper;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedJobMapper;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedTaskMapper;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedWorkflowMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Distributed job query
 *
 * @author Ponfee
 */
@Component
public class DistributedJobQuerier {

    private final SchedJobMapper jobMapper;
    private final SchedTaskMapper taskMapper;
    private final SchedInstanceMapper instanceMapper;
    private final SchedWorkflowMapper workflowMapper;

    public DistributedJobQuerier(SchedJobMapper jobMapper,
                                 SchedTaskMapper taskMapper,
                                 SchedInstanceMapper instanceMapper,
                                 SchedWorkflowMapper workflowMapper) {
        this.jobMapper = jobMapper;
        this.taskMapper = taskMapper;
        this.instanceMapper = instanceMapper;
        this.workflowMapper = workflowMapper;
    }

    public SchedJob getJob(long jobId) {
        return jobMapper.get(jobId);
    }

    public SchedInstance getInstance(long instanceId) {
        return instanceMapper.get(instanceId);
    }

    public SchedInstance getInstance(long jobId, long triggerTime, int runType) {
        return instanceMapper.getByJobIdAndTriggerTimeAndRunType(jobId, triggerTime, runType);
    }

    /**
     * Scan will be triggering sched jobs.
     *
     * @param maxNextTriggerTime the maxNextTriggerTime
     * @param size               the query data size
     * @return will be triggering sched jobs
     */
    public List<SchedJob> findBeTriggeringJob(long maxNextTriggerTime, int size) {
        return jobMapper.findBeTriggering(maxNextTriggerTime, size);
    }

    public List<SchedInstance> findExpireWaitingInstance(Date expireTime, int size) {
        return instanceMapper.findExpireState(RunState.WAITING.value(), expireTime.getTime(), expireTime, size);
    }

    public List<SchedInstance> findExpireRunningInstance(Date expireTime, int size) {
        return instanceMapper.findExpireState(RunState.RUNNING.value(), expireTime.getTime(), expireTime, size);
    }

    public List<SchedInstance> findUnterminatedRetryInstance(long rnstanceId) {
        return instanceMapper.findUnterminatedRetry(rnstanceId);
    }

    public List<SchedTask> findBaseInstanceTasks(long instanceId) {
        return taskMapper.findBaseByInstanceId(instanceId);
    }

    public SchedTask getTask(long taskId) {
        return taskMapper.get(taskId);
    }

    public List<WorkflowPredecessorNodeParam> findWorkflowPredecessorNodes(long wnstanceId, long instanceId) {
        List<SchedWorkflow> workflows = workflowMapper.findByWnstanceId(wnstanceId);
        if (CollectionUtils.isEmpty(workflows)) {
            return null;
        }

        SchedWorkflow curWorkflow = workflows.stream()
            .filter(e -> e.getInstanceId() != null)
            .filter(e -> e.getInstanceId() == instanceId)
            .findAny()
            .orElse(null);
        if (curWorkflow == null) {
            return null;
        }

        if (DAGNode.fromString(curWorkflow.getPreNode()).isStart()) {
            return null;
        }

        DAGNode curNode = DAGNode.fromString(curWorkflow.getCurNode());
        WorkflowGraph workflowGraph = new WorkflowGraph(workflows);
        Map<DAGEdge, SchedWorkflow> predecessors = workflowGraph.predecessors(curNode);
        if (MapUtils.isEmpty(predecessors)) {
            return null;
        }

        return predecessors.values()
            .stream()
            .map(e -> {
                List<SchedTask> tasks = taskMapper.findLargeByInstanceId(e.getInstanceId());
                tasks.sort(Comparator.comparing(SchedTask::getTaskNo));
                return WorkflowPredecessorNodeParam.of(e, tasks);
            })
            .sorted(Comparator.comparing(WorkflowPredecessorNodeParam::getSequence))
            .collect(Collectors.toList());
    }

    public List<SchedTask> findLargeInstanceTasks(long instanceId) {
        return taskMapper.findLargeByInstanceId(instanceId);
    }

    public PageResponse<SchedJobResponse> queryJobForPage(SchedJobPageRequest pageRequest) {
        return pageRequest.query(
            jobMapper::queryPageCount,
            jobMapper::queryPageRecords,
            SchedJobConverter.INSTANCE::convert
        );
    }

    public PageResponse<SchedInstanceResponse> queryInstanceForPage(SchedInstancePageRequest pageRequest) {
        if (pageRequest.getEndTime() != null) {
            pageRequest.setEndTime(Dates.endOfDay(pageRequest.getEndTime()));
        }

        PageResponse<SchedInstanceResponse> pageResponse = pageRequest.query(
            instanceMapper::queryPageCount,
            instanceMapper::queryPageRecords,
            SchedJobConverter.INSTANCE::convert
        );

        if (pageRequest.isParent()) {
            fillIsTreeLeaf(pageResponse.getRows());
        }
        return pageResponse;
    }

    public List<SchedInstanceResponse> listInstanceChildren(long pnstanceId) {
        List<SchedInstanceResponse> rows = instanceMapper.queryByPnstanceId(pnstanceId)
            .stream()
            .map(SchedJobConverter.INSTANCE::convert)
            .collect(Collectors.toList());
        fillIsTreeLeaf(rows);
        return rows;
    }

    public List<Map<String, Object>> searchJob(SchedJobSearchRequest req) {
        return jobMapper.searchJob(req.getGroups(), req.getJobName(), req.getJobId());
    }

    // --------------------------------------------------------------------------private methods

    private void fillIsTreeLeaf(List<SchedInstanceResponse> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        List<Long> instanceIds = list.stream().map(SchedInstanceResponse::getInstanceId).collect(Collectors.toList());
        Map<Long, Integer> map = instanceMapper.queryChildCount(instanceIds)
            .stream()
            .collect(Collectors.toMap(e -> MapUtils.getLongValue(e, "pnstanceId"), e -> MapUtils.getIntValue(e, "count")));
        list.forEach(e -> {
            Integer count = map.get(e.getInstanceId());
            e.setIsTreeLeaf(Numbers.isNullOrZero(count) ? 0 : 1);
        });
    }

}
