package cn.ponfee.scheduler;

/**
 * Application test suite
 *
 * @author Ponfee
 */
/*
@org.junit.runner.RunWith(org.junit.runners.Suite.class)
@org.junit.runners.Suite.SuiteClasses({
    RedisTest.class,
    TimingWheelTest.class
})
*/

/*
@org.junit.runner.RunWith(org.junit.platform.runner.JUnitPlatform.class)
@org.junit.platform.suite.api.SelectPackages({"cn.ponfee.scheduler"})
*/

@org.junit.platform.suite.api.Suite
@org.junit.platform.suite.api.SuiteDisplayName("Scheduler supervisor test suite")
@org.junit.platform.suite.api.SelectPackages("cn.ponfee.scheduler.supervisor")
public class TestAllCase {

}
