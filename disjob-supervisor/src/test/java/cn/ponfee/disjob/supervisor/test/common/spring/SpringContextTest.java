/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.test.common.spring;

import cn.ponfee.disjob.common.util.ClassUtils;
import cn.ponfee.disjob.supervisor.SpringBootTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;

/**
 * @author Ponfee
 */
public class SpringContextTest extends SpringBootTestBase<Object> {

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
    public void testSpringConfig() {
        /*
        // Spring boot 默认加载：application.properties, application.yaml, application.yml
        Assertions.assertEquals(environment.getProperty("application.properties.conf"), "1111");
        Assertions.assertEquals(environment.getProperty("application.yaml.conf"), "2222");
        Assertions.assertEquals(environment.getProperty("application.yml.conf"), "3333");

        // spring.profiles.active: test
        Assertions.assertEquals(environment.getProperty("test.env.foo"), "bar");
        */
        System.out.println(environment);
    }

}
