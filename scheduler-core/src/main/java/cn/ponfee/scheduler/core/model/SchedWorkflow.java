/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.model;

import cn.ponfee.scheduler.common.base.model.BaseEntity;
import cn.ponfee.scheduler.core.enums.RunState;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Scheduler workflow, mapped database table sched_workflow
 *
 * @author Ponfee
 */
@Getter
@Setter
@NoArgsConstructor
public class SchedWorkflow extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 3485414559751420216L;

    /**
     * sched_instance.workflow_instance_id
     */
    private Long workflowInstanceId;

    /**
     * 前置任务节点(section:ordinal:name)
     */
    private String preNodeId;

    /**
     * 当前任务节点(section:ordinal:name)
     */
    private String curNodeId;

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

    public SchedWorkflow(Long workflowInstanceId, String preNodeId, String curNodeId, int sequence, RunState runState) {
        this.workflowInstanceId = workflowInstanceId;
        this.preNodeId = preNodeId;
        this.curNodeId = curNodeId;
        this.sequence = sequence;
        this.runState = runState.value();
    }

}
