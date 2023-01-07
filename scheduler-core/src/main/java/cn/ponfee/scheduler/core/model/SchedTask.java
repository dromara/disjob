/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.model;

import cn.ponfee.scheduler.common.base.model.BaseEntity;
import cn.ponfee.scheduler.core.enums.ExecuteState;
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

    public SchedTask() {
    }

    public SchedTask(String taskParam) {
        this.taskParam = taskParam;
    }

    public SchedTask(long taskId, String worker) {
        this.taskId = taskId;
        this.worker = worker;
    }

    /**
     * 全局唯一ID
     */
    private Long taskId;

    /**
     * sched_track.track_id
     */
    private Long trackId;

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
     * 执行状态：10-等待执行；20-正在执行；30-暂停执行；40-正常完成；50-实例化失败取消；51-校验失败取消；52-初始化异常取消；53-执行失败取消；54-执行异常取消；55-执行超时取消；56-数据不一致取消；57-执行冲突取消(sched_job.collision_strategy=3)；58-手动取消；
     *
     * @see ExecuteState
     */
    private Integer executeState;

    /**
     * 保存的执行快照数据
     */
    private String executeSnapshot;

    /**
     * 工作进程(JVM进程，GROUP:INSTANCE-ID:HOST:PORT)
     */
    private String worker;

    /**
     * 执行错误信息
     */
    private String errorMsg;

    /**
     * Builds sched tasks
     *
     * @param taskParam  the task param
     * @param taskId     the task id
     * @param trackId    the track id
     * @param createTime the created time
     * @return SchedTask
     */
    public static SchedTask from(String taskParam, long taskId, long trackId, Date createTime) {
        SchedTask task = new SchedTask(taskParam == null ? "" : taskParam);
        task.setTaskId(taskId);
        task.setTrackId(trackId);
        task.setExecuteState(ExecuteState.WAITING.value());
        task.setUpdatedAt(createTime);
        task.setCreatedAt(createTime);
        return task;
    }
}
