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

    void deleteByParentJobId(long parentJobId);

    void deleteByChildJobId(long parentJobId);

}