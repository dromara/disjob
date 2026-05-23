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

import cn.ponfee.disjob.core.enums.RunStatus;
import cn.ponfee.disjob.supervisor.application.request.SchedInstancePageRequest;
import cn.ponfee.disjob.supervisor.model.SchedInstance;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static cn.ponfee.disjob.common.spring.TransactionUtils.isOneAffectedRow;

/**
 * Mybatis mapper of sched_instance database table.
 *
 * @author Ponfee
 */
public interface SchedInstanceMapper {

    int insertBatch(List<SchedInstance> instances);

    /**
     * Gets sched instance by instance_id
     *
     * @param instanceId the instance id
     * @return SchedInstance
     */
    SchedInstance get(long instanceId);

    /**
     * Gets job id by instance_id
     *
     * @param instanceId the instance id
     * @return job id
     */
    Long getJobId(long instanceId);

    Long getWnstanceId(long instanceId);

    List<SchedInstance> findChildren(@Param("pnstanceId") long pnstanceId, @Param("runType") Integer runType);

    int start(@Param("instanceId") long instanceId, @Param("runStartTime") Date runStartTime);

    int terminate(@Param("instanceId") long instanceId,
                  @Param("toStatus") int toStatus,
                  @Param("fromStatuses") List<Integer> fromStatuses,
                  @Param("runEndTime") Date runEndTime);

    int updateStatus(@Param("instanceId") long instanceId, @Param("toStatus") int toStatus, @Param("fromStatus") int fromStatus);

    int updateRetrying(@Param("instanceId") long instanceId,
                       @Param("retrying") boolean retrying,
                       @Param("toStatus") int toStatus,
                       @Param("fromStatus") int fromStatus);

    List<SchedInstance> findExpireStatus(@Param("runStatus") int runStatus,
                                         @Param("expireTime") Date expireTime,
                                         @Param("size") int size);

    SchedInstance getRetrying(long instanceId);

    List<SchedInstance> findRunRetry(long instanceId);

    List<SchedInstance> findWorkflowNode(long wnstanceId);

    int updateNextScanTime(@Param("instanceId") long instanceId,
                           @Param("nextScanTime") Date nextScanTime,
                           @Param("version") int version);

    SchedInstance lock(long instanceId);

    /**
     * Delete the sched instance.
     *
     * @param instanceId the instance id
     * @return delete sql affected rows
     */
    int deleteByInstanceId(long instanceId);

    int deleteByWnstanceId(long wnstanceId);

    SchedInstance getByDedupKey(@Param("jobId") long jobId,
                                @Param("triggerTime") long triggerTime,
                                @Param("runType") int runType,
                                @Param("dedupKey") long dedupKey);

    // -------------------------------------------------query for page

    long queryPageCount(SchedInstancePageRequest request);

    List<SchedInstance> queryPageRecords(SchedInstancePageRequest request);

    List<SchedInstance> queryByPnstanceId(long pnstanceId);

    List<Map<String, Object>> queryChildCount(List<Long> pnstanceIds);

    // -------------------------------------------------default methods

    default boolean terminate(long instanceId, RunStatus toStatus, List<Integer> fromStatuses, Date runEndTime) {
        return isOneAffectedRow(terminate(instanceId, toStatus.value(), fromStatuses, runEndTime));
    }

    default boolean updateStatus(long instanceId, RunStatus toStatus, RunStatus fromStatus) {
        return isOneAffectedRow(updateStatus(instanceId, toStatus.value(), fromStatus.value()));
    }

    default boolean updateRetrying(long instanceId, boolean retrying, RunStatus toStatus, RunStatus fromStatus) {
        return isOneAffectedRow(updateRetrying(instanceId, retrying, toStatus.value(), fromStatus.value()));
    }

}
