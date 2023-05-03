/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob;

/**
 * Application test suite
 *
 * @author Ponfee
 */
/*
@org.junit.runner.RunWith(org.junit.runners.Suite.class)
@org.junit.runners.Suite.SuiteClasses({
    cn.ponfee.disjob.supervisor.test.job.util.CommonTest.class,
    cn.ponfee.disjob.supervisor.test.job.dao.SchedJobMapperTest.class,
    cn.ponfee.disjob.supervisor.test.common.transaction.JobTxManagerTest.class
})
*/

/*
@org.junit.runner.RunWith(org.junit.platform.runner.JUnitPlatform.class)
@org.junit.platform.suite.api.SelectPackages({"cn.ponfee.disjob"})
*/

@org.junit.platform.suite.api.Suite
@org.junit.platform.suite.api.SuiteDisplayName("Disjob supervisor test suite")
@org.junit.platform.suite.api.SelectPackages("cn.ponfee.disjob.supervisor")
public class AllTestSuite {

}
