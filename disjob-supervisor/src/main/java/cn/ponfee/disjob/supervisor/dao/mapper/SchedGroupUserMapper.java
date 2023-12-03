/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.dao.mapper;

import cn.ponfee.disjob.core.model.SchedGroupUser;

import java.util.List;

/**
 * Mybatis mapper of sched_user_group database table.
 *
 * @author Ponfee
 */
public interface SchedGroupUserMapper {

    int batchInsert(List<SchedGroupUser> list);

    List<SchedGroupUser> findAll();

    int deleteByGroup(String group);
}
