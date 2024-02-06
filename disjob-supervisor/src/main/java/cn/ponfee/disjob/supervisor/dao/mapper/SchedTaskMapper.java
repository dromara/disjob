/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.dao.mapper;

import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.core.param.supervisor.UpdateTaskWorkerParam;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * Mybatis mapper of sched_task database table.
 *
 * @author Ponfee
 */
public interface SchedTaskMapper {

    int batchInsert(List<SchedTask> records);

    SchedTask get(long taskId);

    List<SchedTask> findBaseByInstanceId(long instanceId);

    List<SchedTask> findLargeByInstanceId(long instanceId);

    int incrementDispatchFailedCount(@Param("taskId") long taskId,
                                     @Param("currentDispatchFailedCount") int currentDispatchFailedCount);

    int start(@Param("taskId") long taskId,
              @Param("worker") String worker,
              @Param("executeStartTime") Date executeStartTime);

    int terminate(@Param("taskId") long taskId,
                  @Param("worker") String worker,
                  @Param("toState") int toState,
                  @Param("fromState") int fromState,
                  @Param("executeEndTime") Date executeEndTime,
                  @Param("errorMsg") String errorMsg);

    int updateStateByInstanceId(@Param("instanceId") long instanceId,
                                @Param("toState") int toState,
                                @Param("fromStateList") List<Integer> fromStateList,
                                @Param("executeEndTime") Date executeEndTime);

    int changeState(@Param("instanceId") long instanceId,
                    @Param("toState") int toState);

    int savepoint(@Param("taskId") long taskId,
                  @Param("executeSnapshot") String executeSnapshot);

    /**
     * Delete the sched task.
     *
     * @param instanceId the instance id
     * @return delete sql affected rows
     */
    int deleteByInstanceId(long instanceId);

    /**
     * Update or clear the task worker
     *
     * @param list the update task worker param list
     * @return update sql affected rows
     */
    int batchUpdateWorker(List<UpdateTaskWorkerParam> list);

}
