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

package cn.ponfee.disjob.core.model;

import cn.ponfee.disjob.common.dag.DAGEdge;
import cn.ponfee.disjob.common.dag.DAGNode;
import cn.ponfee.disjob.common.model.BaseEntity;
import cn.ponfee.disjob.core.enums.RunState;
import lombok.Getter;
import lombok.Setter;

import java.beans.Transient;

/**
 * Disjob workflow, mapped database table sched_workflow
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedWorkflow extends BaseEntity {
    private static final long serialVersionUID = 3485414559751420216L;

    /**
     * sched_instance.wnstance_id
     */
    private Long wnstanceId;

    /**
     * 前置任务节点(section:ordinal:name)
     */
    private String preNode;

    /**
     * 当前任务节点(section:ordinal:name)
     */
    private String curNode;

    /**
     * 运行状态：10-待运行；20-运行中；30-已暂停；40-已完成；50-已取消；
     *
     * @see RunState
     */
    private Integer runState;

    /**
     * 当前执行的sched_instance.instance_id(失败重试时会更新为重试的instance_id)
     */
    private Long instanceId;

    public static SchedWorkflow of(Long wnstanceId, String preNode, String curNode) {
        SchedWorkflow workflow = new SchedWorkflow();
        workflow.setWnstanceId(wnstanceId);
        workflow.setPreNode(preNode);
        workflow.setCurNode(curNode);
        workflow.setRunState(RunState.WAITING.value());
        return workflow;
    }

    public DAGEdge toEdge() {
        return DAGEdge.of(preNode, curNode);
    }

    public DAGNode parseCurNode() {
        return DAGNode.fromString(curNode);
    }

    public DAGNode parsePreNode() {
        return DAGNode.fromString(preNode);
    }

    @Transient
    public boolean isTerminal() {
        return RunState.of(runState).isTerminal();
    }

    @Transient
    public boolean isFailure() {
        return RunState.of(runState).isFailure();
    }

    @Transient
    public boolean isRunning() {
        return RunState.RUNNING.equalsValue(runState);
    }

    @Transient
    public boolean isWaiting() {
        return RunState.WAITING.equalsValue(runState);
    }

    @Transient
    public boolean isPaused() {
        return RunState.PAUSED.equalsValue(runState);
    }

}
