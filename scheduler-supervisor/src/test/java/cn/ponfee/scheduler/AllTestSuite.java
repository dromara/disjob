/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler;

/**
 * Application test suite
 *
 * @author Ponfee
 */
/*
@org.junit.runner.RunWith(org.junit.runners.Suite.class)
@org.junit.runners.Suite.SuiteClasses({
    cn.ponfee.scheduler.supervisor.test.job.util.CommonTest.class,
    cn.ponfee.scheduler.supervisor.test.job.dao.SchedJobMapperTest.class,
    cn.ponfee.scheduler.supervisor.test.common.transaction.JobTxManagerTest.class
})
*/

/*
@org.junit.runner.RunWith(org.junit.platform.runner.JUnitPlatform.class)
@org.junit.platform.suite.api.SelectPackages({"cn.ponfee.scheduler"})
*/

@org.junit.platform.suite.api.Suite
@org.junit.platform.suite.api.SuiteDisplayName("Scheduler supervisor test suite")
@org.junit.platform.suite.api.SelectPackages("cn.ponfee.scheduler.supervisor")
public class AllTestSuite {

}
