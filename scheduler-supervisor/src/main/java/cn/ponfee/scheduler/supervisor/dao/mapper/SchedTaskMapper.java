/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.dao.mapper;

import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.core.param.TaskWorker;
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

    SchedTask getByTaskId(long taskId);

    List<SchedTask> findMediumByInstanceId(long instanceId);

    List<SchedTask> findLargeByInstanceId(long instanceId);

    int start(@Param("taskId") long taskId,
              @Param("worker") String worker,
              @Param("executeStartTime") Date executeStartTime);

    int terminate(@Param("taskId") long taskId,
                  @Param("toState") int toState,
                  @Param("fromState") int fromState,
                  @Param("executeEndTime") Date executeEndTime,
                  @Param("errorMsg") String errorMsg);

    int updateStateByInstanceId(@Param("instanceId") long instanceId,
                                @Param("toState") int toState,
                                @Param("fromStateList") List<Integer> fromStateList,
                                @Param("executeEndTime") Date executeEndTime);

    int forceChangeState(@Param("instanceId") long instanceId,
                         @Param("toState") int toState);

    int checkpoint(@Param("taskId") long taskId,
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
     * @param list the data
     * @return update sql affected rows
     */
    int batchUpdateWorker(List<TaskWorker> list);

}
