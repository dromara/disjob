/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.handle.execution;

import cn.ponfee.disjob.core.model.SchedTask;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Executing task
 *
 * @author Ponfee
 */
@Getter
@Setter
public class ExecutingTask extends AbstractExecutionTask {
    private static final long serialVersionUID = 8910065837652403459L;

    /**
     * sched_job.job_id
     */
    private Long jobId;

    /**
     * sched_instance.instance_id
     */
    private Long instanceId;

    /**
     * sched_instance.wnstance_id
     * <p>非工作流任务时值为null
     */
    private Long wnstanceId;

    /**
     * job_handler执行task的参数
     */
    private String taskParam;

    /**
     * 工作流(DAG)任务的前驱节点列表数据
     */
    private List<WorkflowPredecessorNode> workflowPredecessorNodes;

    public static ExecutingTask of(Long jobId,
                                   Long wnstanceId,
                                   SchedTask task,
                                   List<WorkflowPredecessorNode> workflowPredecessorNodes) {
        ExecutingTask executingTask = ExecutionTaskConverter.INSTANCE.toExecutingTask(task);
        executingTask.setJobId(jobId);
        executingTask.setWnstanceId(wnstanceId);
        executingTask.setWorkflowPredecessorNodes(workflowPredecessorNodes);
        return executingTask;
    }

}
