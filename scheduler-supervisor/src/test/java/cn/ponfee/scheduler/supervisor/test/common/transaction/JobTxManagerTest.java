package cn.ponfee.scheduler.supervisor.test.common.transaction;

import cn.ponfee.scheduler.common.base.tuple.Tuple2;
import cn.ponfee.scheduler.supervisor.config.JobTxManagerTestService;
import cn.ponfee.scheduler.supervisor.dao.mapper.SchedJobMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * test job transaction
 *
 * @author Ponfee
 */
public class JobTxManagerTest extends AbstractTxManagerTest<JobTxManagerTestService, Long> {

    public JobTxManagerTest(@Autowired SchedJobMapper schedJobMapper) {
        super(() -> {
            List<Long> list = schedJobMapper.testListLimit(2);
            if (list.size() < 2) {
                throw new IllegalStateException("Not find enough sched job data.");
            }
            return Tuple2.of(list.get(0), list.get(1));
        });
    }

}
