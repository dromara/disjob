/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.dao.mapper;

import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.supervisor.application.request.SchedJobPageRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Mybatis mapper of sched_job database table.
 *
 * @author Ponfee
 */
public interface SchedJobMapper {

    // ----------------------------use in test start
    List<SchedJob> testFindByJobIds(List<Long> jobIds);
    int testUpdateRemark(@Param("jobId") Long jobId, @Param("remark") String remark);
    List<Long> testListLimit(int limit);
    // ----------------------------use in test end

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

    // -------------------------------------------------query for page

    long queryPageCount(SchedJobPageRequest request);

    List<SchedJob> queryPageRecords(SchedJobPageRequest request);
}
