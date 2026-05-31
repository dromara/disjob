/*
 * Copyright 2022-2026 Ponfee (http://www.ponfee.cn/)
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

package cn.ponfee.disjob.supervisor.dao.mapper;

import cn.ponfee.disjob.core.enums.ExecuteStatus;
import cn.ponfee.disjob.supervisor.model.SchedTask;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

import static cn.ponfee.disjob.common.spring.TransactionUtils.isOneAffectedRow;

/**
 * Mybatis mapper of sched_task database table.
 *
 * @author Ponfee
 */
public interface SchedTaskMapper {

    int insertBatch(List<SchedTask> records);

    SchedTask get(long taskId);

    List<SchedTask> findBaseByInstanceIdAndStatuses(@Param("instanceId") long instanceId, @Param("statuses") List<Integer> statuses);

    List<SchedTask> findLargeByInstanceIdAndStatuses(@Param("instanceId") long instanceId, @Param("statuses") List<Integer> statuses);

    int incrementDispatchFailures(@Param("taskId") long taskId, @Param("currentDispatchFailures") int currentDispatchFailures);

    int start(@Param("taskId") long taskId,
              @Param("worker") String worker,
              @Param("idempotencyKey") String idempotencyKey,
              @Param("executeStartTime") Date executeStartTime);

    boolean checkIdempotentKey(@Param("taskId") long taskId,
                               @Param("worker") String worker,
                               @Param("idempotencyKey") String idempotencyKey);

    int terminate(@Param("taskId") long taskId,
                  @Param("worker") String worker,
                  @Param("toStatus") int toStatus,
                  @Param("fromStatus") int fromStatus,
                  @Param("executeEndTime") Date executeEndTime,
                  @Param("errorMsg") String errorMsg);

    int updateStatusByInstanceId(@Param("instanceId") long instanceId,
                                 @Param("toStatus") int toStatus,
                                 @Param("fromStatuses") List<Integer> fromStatuses,
                                 @Param("executeEndTime") Date executeEndTime);

    int forceChangeStatus(@Param("instanceId") long instanceId, @Param("toStatus") int toStatus);

    int savepoint(@Param("taskId") long taskId, @Param("worker") String worker, @Param("executionData") String executionData);

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
     * @param taskIds the task id list
     * @param worker  the worker
     * @return update sql affected rows
     */
    int updateWorkerBatch(@Param("taskIds") List<Long> taskIds, @Param("worker") String worker);

    // -------------------------------------------------default methods

    default boolean terminate(long taskId, String worker, ExecuteStatus to, ExecuteStatus from, Date executeEndTime, String errorMsg) {
        return isOneAffectedRow(terminate(taskId, worker, to.value(), from.value(), executeEndTime, errorMsg));
    }

    default List<SchedTask> findBaseByInstanceId(long instanceId) {
        return findBaseByInstanceIdAndStatuses(instanceId, null);
    }

    default List<SchedTask> findLargeByInstanceId(long instanceId) {
        return findLargeByInstanceIdAndStatuses(instanceId, null);
    }
}
