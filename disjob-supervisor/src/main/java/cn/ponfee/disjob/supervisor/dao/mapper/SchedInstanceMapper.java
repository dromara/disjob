/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
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

import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.supervisor.application.request.SchedInstancePageRequest;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Mybatis mapper of sched_instance database table.
 *
 * @author Ponfee
 */
public interface SchedInstanceMapper {

    int insert(SchedInstance instance);

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

    Long getPnstanceId(@Param("instanceId") long instanceId,
                       @Param("runType") Integer runType);

    int start(@Param("instanceId") long instanceId,
              @Param("runStartTime") Date runStartTime);

    int terminate(@Param("instanceId") long instanceId,
                  @Param("toState") int toState,
                  @Param("fromStateList") List<Integer> fromStateList,
                  @Param("runEndTime") Date runEndTime);

    int updateState(@Param("instanceId") long instanceId,
                    @Param("toState") int toState,
                    @Param("fromState") int fromState);

    List<SchedInstance> findExpireState(@Param("runState") int runState,
                                        @Param("expireTime") Date expireTime,
                                        @Param("size") int size);

    List<SchedInstance> findUnterminatedRetry(long rnstanceId);

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

    SchedInstance getByUniqueKey(@Param("jobId") long jobId,
                                 @Param("triggerTime") long triggerTime,
                                 @Param("runType") int runType,
                                 @Param("uniqueFlag") long uniqueFlag);

    // -------------------------------------------------query for page

    long queryPageCount(SchedInstancePageRequest request);

    List<SchedInstance> queryPageRecords(SchedInstancePageRequest request);

    List<SchedInstance> queryByPnstanceId(long pinstanceId);

    List<Map<String, Object>> queryChildCount(List<Long> pinstanceIds);
}
