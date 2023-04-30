/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.dao.mapper;

import cn.ponfee.scheduler.core.model.SchedInstance;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * Mybatis mapper of sched_instance database table.
 *
 * @author Ponfee
 */
public interface SchedInstanceMapper {

    int insert(SchedInstance record);

    /**
     * Gets sched instance by instance_id
     *
     * @param instanceId the instance id
     * @return SchedInstance
     */
    SchedInstance getByInstanceId(long instanceId);

    Long getWorkflowInstanceId(long instanceId);

    int start(@Param("instanceId") long instanceId,
              @Param("runStartTime") Date runStartTime);

    int terminate(@Param("instanceId") long instanceId,
                  @Param("toState") int toState,
                  @Param("fromStateList") List<Integer> fromStateList,
                  @Param("runEndTime") Date runEndTime);

    int updateState(@Param("instanceId") long instanceId,
                    @Param("toState") int toState,
                    @Param("fromState") int fromState);

    int forceChangeState(@Param("instanceId") long instanceId,
                         @Param("toState") int toState);

    List<SchedInstance> findExpireState(@Param("runState") int runState,
                                        @Param("expireTime") long expireTime,
                                        @Param("updateTime") Date updateTime,
                                        @Param("size") int size);

    List<SchedInstance> findUnterminatedRetry(long rootInstanceId);

    List<SchedInstance> findWorkflowNode(long workflowInstanceId);

    int renewUpdateTime(@Param("instanceId") long instanceId,
                        @Param("updateTime") Date updateTime,
                        @Param("version") int version);

    SchedInstance lock(long instanceId);

    /**
     * Delete the sched instance.
     *
     * @param instanceId the instance id
     * @return delete sql affected rows
     */
    int deleteByInstanceId(long instanceId);

    int deleteByWorkflowInstanceId(long workflowInstanceId);

    SchedInstance getByJobIdAndTriggerTimeAndRunType(@Param("jobId") long jobId,
                                                     @Param("triggerTime") long triggerTime,
                                                     @Param("runType") int runType);
}
