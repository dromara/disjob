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
import cn.ponfee.disjob.supervisor.application.value.TokenName;
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

    int updateToken(@Param("group") String group,
                    @Param("name") TokenName name,
                    @Param("newToken") String newToken,
                    @Param("updatedBy") String updatedBy,
                    @Param("oldToken") String oldToken);

    int updateOwnUser(@Param("group") String group,
                      @Param("ownUser") String ownUser,
                      @Param("updatedBy") String updatedBy);

    int softDelete(@Param("group") String group,
                   @Param("updatedBy") String updatedBy);

    int edit(SchedGroup schedGroup);

    boolean exists(String group);

    List<String> matchGroup(String term);

    // -------------------------------------------------query for page

    long queryPageCount(SchedGroupPageRequest request);

    List<SchedGroup> queryPageRecords(SchedGroupPageRequest request);
}
