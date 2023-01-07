/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.dao.mapper;

import cn.ponfee.scheduler.core.model.SchedDepend;

import java.util.List;

/**
 * Mybatis mapper of sched_depend database table.
 *
 * @author Ponfee
 */
public interface SchedDependMapper {

    int insertBatch(List<SchedDepend> records);

    List<SchedDepend> findByParentJobId(long parentJobId);

    int deleteByParentJobId(long parentJobId);

    int deleteByChildJobId(long parentJobId);

}