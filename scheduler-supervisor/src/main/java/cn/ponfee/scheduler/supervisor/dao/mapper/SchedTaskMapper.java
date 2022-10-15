package cn.ponfee.scheduler.supervisor.dao.mapper;

import cn.ponfee.scheduler.core.model.SchedTask;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * Mybatis mapper of sched_task database table.
 *
 * @author Ponfee
 */
public interface SchedTaskMapper {

    int insertBatch(List<SchedTask> records);

    SchedTask getByTaskId(long taskId);

    List<SchedTask> getByTrackId(long trackId);

    List<SchedTask> findByTrackId(long trackId);

    int start(@Param("taskId") long taskId,
              @Param("worker") String worker,
              @Param("executeStartTime") Date executeStartTime);

    int terminate(@Param("taskId") long taskId,
                  @Param("toState") int toState,
                  @Param("fromState") int fromState,
                  @Param("executeEndTime") Date executeEndTime,
                  @Param("errorMsg") String errorMsg);

    int updateState(@Param("taskId") long taskId,
                    @Param("toState") int toState,
                    @Param("fromState") int fromState,
                    @Param("errorMsg") String errorMsg,
                    @Param("version") Integer version);

    int forceUpdateState(@Param("trackId") long trackId,
                         @Param("targetState") int targetState);

    int updateStateByTrackId(@Param("trackId") long trackId,
                             @Param("toState") int toState,
                             @Param("fromStateList") List<Integer> fromStateList,
                             @Param("executeEndTime") Date executeEndTime);

    int checkpoint(@Param("taskId") long taskId,
                   @Param("executeSnapshot") String executeSnapshot);

    int updateErrorMsg(@Param("taskId") long taskId,
                       @Param("errorMsg") String errorMsg);

    /**
     * Delete the sched task.
     *
     * @param trackId the track id
     * @return delete sql affected rows
     */
    int deleteByTrackId(long trackId);

}
