/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.dao.mapper;

import cn.ponfee.disjob.core.model.SchedGroup;
import cn.ponfee.disjob.supervisor.application.request.SchedGroupPageRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Mybatis mapper of sched_group database table.
 *
 * @author Ponfee
 */
public interface SchedGroupMapper {

    int insert(SchedGroup schedGroup);

    SchedGroup get(String group);

    List<SchedGroup> findAll();

    int update(SchedGroup schedGroup);

    int updateSupervisorToken(@Param("group") String group,
                              @Param("newSupervisorToken") String newSupervisorToken,
                              @Param("oldSupervisorToken") String oldSupervisorToken);

    int updateWorkerToken(@Param("group") String group,
                          @Param("newWorkerToken") String newWorkerToken,
                          @Param("oldWorkerToken") String oldWorkerToken);

    int updateUserToken(@Param("group") String group,
                        @Param("newUserToken") String newUserToken,
                        @Param("oldUserToken") String oldUserToken);

    int softDelete(String group);

    boolean exists(String group);

    // -------------------------------------------------query for page

    long queryPageCount(SchedGroupPageRequest request);

    List<SchedGroup> queryPageRecords(SchedGroupPageRequest request);
}
