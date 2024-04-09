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

import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.supervisor.application.request.SchedJobPageRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mybatis mapper of sched_job database table.
 *
 * @author Ponfee
 */
public interface SchedJobMapper {

    List<SchedJob> findByJobIds(List<Long> jobIds);

    int insert(SchedJob job);

    int update(SchedJob job);

    /**
     * Gets sched job by job_id
     *
     * @param jobId the job id
     * @return SchedJob
     */
    SchedJob get(long jobId);

    /**
     * Exists group and job name
     *
     * @param group   the group
     * @param jobName the job name
     * @return {@code true} if exists
     */
    boolean exists(@Param("group") String group,
                   @Param("jobName") String jobName);

    /**
     * Gets group by job_id
     *
     * @param jobId the job id
     * @return group
     */
    String getGroup(long jobId);

    /**
     * Finds job witch will be triggering
     *
     * @param maxNextTriggerTime the max next trigger time
     * @param size               the size
     * @return jobs
     */
    List<SchedJob> findBeTriggering(@Param("maxNextTriggerTime") long maxNextTriggerTime,
                                    @Param("size") int size);

    int updateNextScanTime(SchedJob schedJob);

    /**
     * Disable the job.
     *
     * @param job the job
     * @return update sql affected rows
     */
    int disable(SchedJob job);

    /**
     * Update the job next trigger time.
     *
     * @param job the job
     * @return update sql affected rows
     */
    int updateNextTriggerTime(SchedJob job);

    /**
     * Updates job state
     *
     * @param jobId           the job id
     * @param nextTriggerTime the next trigger time
     * @return update sql affected rows
     */
    int updateFixedDelayNextTriggerTime(@Param("jobId") long jobId,
                                        @Param("nextTriggerTime") long nextTriggerTime);

    /**
     * Updates job state
     *
     * @param jobId     the job id
     * @param toState   the target state
     * @param fromState the source state
     * @return update sql affected rows
     */
    int updateState(@Param("jobId") long jobId,
                    @Param("toState") int toState,
                    @Param("fromState") int fromState);

    /**
     * Soft delete the job.
     *
     * @param jobId the job id
     * @return delete sql affected rows
     */
    int softDelete(long jobId);

    List<Map<String, Object>> searchJob(@Param("groups") Set<String> groups,
                                        @Param("jobName") String jobName,
                                        @Param("jobId") Long jobId);

    // -------------------------------------------------query for page

    long queryPageCount(SchedJobPageRequest request);

    List<SchedJob> queryPageRecords(SchedJobPageRequest request);
}
