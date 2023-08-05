/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.model;

import cn.ponfee.disjob.common.model.BaseEntity;
import cn.ponfee.disjob.core.enums.ExecuteState;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * The schedule task entity, mapped database table sched_task
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedTask extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 4882055618593707631L;

    /**
     * 全局唯一ID
     */
    private Long taskId;

    /**
     * sched_instance.instance_id
     *
     * @see SchedInstance#getInstanceId()
     */
    private Long instanceId;

    /**
     * 任务序号(从1开始)
     */
    private Integer taskNo;

    /**
     * 任务总数量
     */
    private Integer taskCount;

    /**
     * job_handler执行task的参数(参考sched_job.job_param)
     */
    private String taskParam;

    /**
     * 执行开始时间
     */
    private Date executeStartTime;

    /**
     * 执行结束时间
     */
    private Date executeEndTime;

    /**
     * 执行时长(毫秒)
     */
    private Long executeDuration;

    /**
     * 执行状态：10-等待执行；20-正在执行；30-暂停执行；40-执行完成；50-实例化异常；51-校验失败；52-初始化异常；53-执行失败；54-执行异常；55-执行超时；56-执行冲突(sched_job.collided_strategy=3)；57-手动取消；58-广播未执行；
     *
     * @see ExecuteState
     */
    private Integer executeState;

    /**
     * 保存的执行快照(状态)数据
     */
    private String executeSnapshot;

    /**
     * 工作进程(JVM进程，GROUP:WORKER-ID:HOST:PORT)
     */
    private String worker;

    /**
     * 执行错误信息
     */
    private String errorMsg;

    /**
     * 行记录版本号
     */
    private Integer version;

    /**
     * Creates sched tasks
     *
     * @param taskParam  the task param
     * @param taskId     the task id
     * @param instanceId the instance id
     * @param taskNo     the task no
     * @param taskCount  the task count
     * @param createTime the created time
     * @param worker     the worker
     * @return SchedTask
     */
    public static SchedTask create(String taskParam, long taskId, long instanceId,
                                   int taskNo, int taskCount, Date createTime, String worker) {
        SchedTask task = new SchedTask();
        task.setTaskParam(taskParam == null ? "" : taskParam);
        task.setTaskId(taskId);
        task.setInstanceId(instanceId);
        task.setTaskNo(taskNo);
        task.setTaskCount(taskCount);
        task.setWorker(worker);
        task.setExecuteState(ExecuteState.WAITING.value());
        task.setUpdatedAt(createTime);
        task.setCreatedAt(createTime);
        return task;
    }
}
