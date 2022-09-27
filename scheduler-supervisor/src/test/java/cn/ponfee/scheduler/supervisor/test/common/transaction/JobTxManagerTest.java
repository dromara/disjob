package cn.ponfee.scheduler.supervisor.test.common.transaction;

import cn.ponfee.scheduler.supervisor.config.JobTxManagerTestService;

/**
 * test job transaction
 *
 * @author Ponfee
 */
public class JobTxManagerTest extends AbstractTxManagerTest<JobTxManagerTestService, Long> {

    public JobTxManagerTest() {
        super(1536312529104445449L, 1536312529104445450L);
    }

}
