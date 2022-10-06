package cn.ponfee.scheduler.supervisor.test.common.spring;

import cn.ponfee.scheduler.common.util.ClassUtils;
import cn.ponfee.scheduler.supervisor.SpringBootTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;

/**
 * @author Ponfee
 */
public class SpringContextTest extends SpringBootTestBase {

    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private Environment environment;

    @Test
    public void testClassName() {
        String[] beanDefinitionNames = applicationContext.getBeanNamesForType(Object.class, false, true);
        for (String beanDefinitionName : beanDefinitionNames) {
            String className = ClassUtils.getName(applicationContext.getBean(beanDefinitionName).getClass());
            if (!className.contains("/")) {
                // exclude such as org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration$$Lambda$1323/394591403
                Assertions.assertTrue(ClassUtils.QUALIFIED_CLASS_NAME_PATTERN.matcher(className).matches(), className);
            } else {
                System.out.println(className);
            }
        }
    }

    @Test
    public void testConfig() {
        Assertions.assertEquals(environment.getProperty("application.properties.conf"), "test123");
        Assertions.assertEquals(environment.getProperty("test.env.foo"), "bar");
    }

}
