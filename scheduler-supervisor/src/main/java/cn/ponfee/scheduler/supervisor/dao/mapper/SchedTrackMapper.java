/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.dao.mapper;

import cn.ponfee.scheduler.core.model.SchedTrack;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * Mybatis mapper of sched_track database table.
 *
 * @author Ponfee
 */
public interface SchedTrackMapper {

    int insert(SchedTrack record);

    /**
     * Gets sched track by track_id
     *
     * @param trackId the track id
     * @return SchedTrack
     */
    SchedTrack getByTrackId(long trackId);

    Integer getStateByTrackId(long trackId);

    int start(@Param("trackId") long trackId,
              @Param("runStartTime") Date runStartTime);

    int terminate(@Param("trackId") long trackId,
                  @Param("toState") int toState,
                  @Param("fromStateList") List<Integer> fromStateList,
                  @Param("runEndTime") Date runEndTime);

    int updateState(@Param("trackId") long trackId,
                    @Param("toState") int toState,
                    @Param("fromState") int fromState,
                    @Param("version") Integer version);

    int forceUpdateState(@Param("trackId") long trackId,
                         @Param("targetState") int targetState);

    List<SchedTrack> findExpireState(@Param("runState") int runState,
                                     @Param("expireTime") long expireTime,
                                     @Param("maxUpdateTime") Date maxUpdateTime,
                                     @Param("size") int size);

    List<SchedTrack> findUnterminatedRetry(long parentTrackId);

    int renewUpdateTime(@Param("trackId") long trackId,
                        @Param("updateTime") Date updateTime,
                        @Param("version") int version);

    Long lockAndGetId(long trackId);

    Integer lockAndGetState(long trackId);

    /**
     * Delete the sched track.
     *
     * @param trackId the track id
     * @return delete sql affected rows
     */
    int deleteByTrackId(long trackId);

    SchedTrack getByTriggerTime(@Param("jobId") long jobId,
                                @Param("triggerTime") long triggerTime,
                                @Param("runType") int runType);
}
