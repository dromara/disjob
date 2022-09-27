package cn.ponfee.scheduler.supervisor.dao.mapper;

import cn.ponfee.scheduler.core.model.SchedJob;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * Mybatis mapper of sched_job database table.
 *
 * @author Ponfee
 */
public interface SchedJobMapper {

    // ----------------------------use in test start
    List<SchedJob> testFindByJobIds(List<Long> jobIds);

    int updateRemark(@Param("jobId") Long jobId, @Param("remark") String remark);
    // ----------------------------use in test end

    List<SchedJob> findByJobIds(@Param("jobIds") List<Long> jobIds);

    int insert(SchedJob record);

    int updateByJobId(SchedJob record);

    /**
     * Gets sched job by job_id
     *
     * @param jobId the job id
     * @return SchedJob
     */
    SchedJob getByJobId(long jobId);

    /**
     * Finds job witch will be triggering
     *
     * @param maxNextTriggerTime the max next trigger time
     * @param size               the size
     * @return jobs
     */
    List<SchedJob> findBeTriggering(@Param("maxNextTriggerTime") long maxNextTriggerTime, 
                                    @Param("size") int size);

    int updateNextScanTime(@Param("jobId") long jobId,
                           @Param("nextScanTime") Date nextScanTime,
                           @Param("version") int version);

    /**
     * Stop the job.
     *
     * @param job the job
     * @return update sql affected rows
     */
    int stop(SchedJob job);

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
     * @param jobId      the job id
     * @param toState   the target state
     * @param fromState the source state
     * @return update sql affected rows
     */
    int updateState(@Param("jobId") long jobId,
                    @Param("toState") int toState,
                    @Param("fromState") int fromState);

    /**
     * Delete the job.
     * 
     * @param jobId the job id
     * @return delete sql affected rows
     */
    int deleteByJobId(long jobId);

    int countJobIds(List<Long> jobIds);
}
