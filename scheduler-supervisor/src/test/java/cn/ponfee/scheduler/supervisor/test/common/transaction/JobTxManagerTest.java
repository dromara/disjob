package cn.ponfee.scheduler.supervisor.test.common.transaction;

import cn.ponfee.scheduler.supervisor.config.JobTxManagerTestService;

/**
 * test job transaction
 *
 * @author Ponfee
 */
public class JobTxManagerTest extends AbstractTxManagerTest<JobTxManagerTestService, Long> {

    public JobTxManagerTest() {
        super(2212719247360L, 2212765908992L);
    }

}
