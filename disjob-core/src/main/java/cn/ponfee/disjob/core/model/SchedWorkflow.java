/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.model;

import cn.ponfee.disjob.common.graph.DAGEdge;
import cn.ponfee.disjob.common.model.BaseEntity;
import cn.ponfee.disjob.core.enums.RunState;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.beans.Transient;
import java.io.Serializable;

/**
 * Disjob workflow, mapped database table sched_workflow
 *
 * @author Ponfee
 */
@Getter
@Setter
@NoArgsConstructor
public class SchedWorkflow extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 3485414559751420216L;

    /**
     * sched_instance.wnstance_id
     */
    private Long wnstanceId;

    /**
     * 当前任务节点(section:ordinal:name)
     */
    private String curNode;

    /**
     * 前置任务节点(section:ordinal:name)
     */
    private String preNode;

    /**
     * 序号(从1开始)
     */
    private Integer sequence;

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

    public SchedWorkflow(Long wnstanceId, String curNode, String preNode, int sequence) {
        this.wnstanceId = wnstanceId;
        this.curNode = curNode;
        this.preNode = preNode;
        this.sequence = sequence;
        this.runState = RunState.WAITING.value();
    }

    public DAGEdge toEdge() {
        return DAGEdge.of(preNode, curNode);
    }

    @Transient
    public boolean isTerminal() {
        return RunState.of(runState).isTerminal();
    }

    @Transient
    public boolean isFailure() {
        return RunState.of(runState).isFailure();
    }

}
