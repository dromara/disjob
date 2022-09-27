package cn.ponfee.scheduler.core.model;

import cn.ponfee.scheduler.common.base.model.BaseEntity;
import cn.ponfee.scheduler.core.enums.RunState;
import cn.ponfee.scheduler.core.enums.RunType;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * The schedule track entity, mapped database table sched_track
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedTrack extends BaseEntity implements Serializable {

    private static final long serialVersionUID = -1457861792948169949L;

    /**
     * 全局唯一ID
     */
    private Long trackId;

    /**
     * run_type IN (DEPEND, RETRY)时的父ID
     */
    private Long parentTrackId;

    /**
     * sched_job.job_id
     */
    private Long jobId;

    /**
     * 触发时间(毫秒时间戳)
     */
    private Long triggerTime;

    /**
     * 运行类型：1-SCHEDULE；2-DEPEND；3-RETRY；4-MANUAL；
     *
     * @see RunType
     */
    private Integer runType;

    /**
     * 运行状态：10-待运行；20-运行中；30-已暂停；40-已完成；50-已取消；
     *
     * @see RunState
     */
    private Integer runState;

    /**
     * 运行开始时间
     */
    private Date runStartTime;

    /**
     * 运行结束时间
     */
    private Date runEndTime;

    /**
     * 运行时长(毫秒)
     */
    private Long runDuration;

    /**
     * 已重试的次数(the maximum value is sched_job.retry_count)
     */
    private Integer retriedCount;

}
