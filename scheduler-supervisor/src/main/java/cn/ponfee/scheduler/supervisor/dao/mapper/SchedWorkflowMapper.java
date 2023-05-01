/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.dao.mapper;

import cn.ponfee.scheduler.core.model.SchedWorkflow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Mybatis mapper of sched_workflow database table.
 *
 * @author Ponfee
 */
public interface SchedWorkflowMapper {

    int batchInsert(List<SchedWorkflow> records);

    List<SchedWorkflow> findByWnstanceId(long wnstanceId);

    int update(@Param("wnstanceId") long wnstanceId,
               @Param("curNode") String curNode,
               @Param("toState") Integer toState,
               @Param("toInstanceId") Long toInstanceId,
               @Param("fromStates") List<Integer> fromStates,
               @Param("fromInstanceId") Long fromInstanceId);

    int resumeWaiting(long wnstanceId);

    int deleteByWnstanceId(long wnstanceId);
}
