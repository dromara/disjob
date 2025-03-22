/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.supervisor.base;

import cn.ponfee.disjob.supervisor.SpringBootTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;
import java.util.regex.Pattern;

/**
 * @author Ponfee
 */
public class SpringContextTest extends SpringBootTestBase<Object> {

    public static final Pattern QUALIFIED_CLASS_NAME_PATTERN = Pattern.compile("^([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*[a-zA-Z_$][a-zA-Z\\d_$]*$");

    @Resource
    private Environment environment;

    @Test
    public void testClassName() {
        String[] beanDefinitionNames = applicationContext.getBeanNamesForType(Object.class, false, true);
        for (String beanDefinitionName : beanDefinitionNames) {
            String className = applicationContext.getBean(beanDefinitionName).getClass().getName();
            if (!className.contains("/")) {
                // exclude such as org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration$$Lambda$1323/394591403
                Assertions.assertTrue(QUALIFIED_CLASS_NAME_PATTERN.matcher(className).matches(), className);
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
