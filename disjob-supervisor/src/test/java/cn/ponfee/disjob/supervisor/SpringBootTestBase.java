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

package cn.ponfee.disjob.supervisor;

import cn.ponfee.disjob.common.util.GenericUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;

/**
 * <pre>
 * Spring boot test base class
 *
 * TestInstance：可以在非static方法上加@BeforeAll/@AfterAll注解
 *   LifeCycle.PER_METHOD(默认)：每个测试方法都会创建一个新的测试类实例；
 *   Lifecycle.PER_CLASS：所有测试方法只创建一个测试类的实例；
 * </pre>
 *
 * @param <T> bean type
 * @author Ponfee
 */

/*
@org.junit.runner.RunWith(org.springframework.test.context.junit4.SpringRunner.class)
@SpringBootTest(classes = SpringBootTestApplication.class)
*/

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    classes = SpringBootTestApplication.class
)
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//@ContextConfiguration(classes = { XXX.class })
//@ActiveProfiles({"DEV"})
public abstract class SpringBootTestBase<T> extends MockitoTestBase implements ApplicationContextAware {

    private final String beanName;
    protected ApplicationContext applicationContext;
    protected T bean;

    public SpringBootTestBase() {
        this(null);
    }

    public SpringBootTestBase(String beanName) {
        this.beanName = beanName;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    // --------------------------------------annotated junit jupiter methods definition

    /**
     * 在当前测试类的所有测试方法之前执行一次，必须是void返回类型且不能为private，可以按junit jupiter的规范带上参数
     * <p>超类的`@BeforeAll`方法将在当前类的方法之前运行，除非它们在当前类中被遮蔽(shadowed)
     * <p>`beforeClass`与`beforeAllMethod`不存在固定的先后执行顺序，当更换方法名后两个的先后执行顺序可能会改变
     */
    /*
    @BeforeAll
    public static void beforeClass() {
        System.out.println("Before test class: " + Dates.format(new Date(), Dates.DATEFULL_PATTERN));
    }
    */

    /**
     * 在当前测试类的所有测试方法之前执行一次：@TestInstance(TestInstance.Lifecycle.PER_CLASS)
     */
    @BeforeAll
    public final void beforeAllMethod() {
        log.info("Before all test method.");
        Class<T> type = GenericUtils.getActualTypeArgument(getClass(), 0);
        if (!Arrays.asList(Void.class, Object.class).contains(type)) {
            this.bean = StringUtils.isBlank(beanName)
                ? applicationContext.getBean(type)
                : applicationContext.getBean(beanName, type);
        }
        SpringBootTestCollector.collect(applicationContext, getClass());

        beforeAll();
    }

    @BeforeEach
    public final void beforeEachMethod() {
        log.info("Before each test method.");
        super.initMock();
        beforeEach();
    }

    @AfterEach
    public final void afterEachMethod() {
        log.info("After each test method.");
        afterEach();
        super.destroyMock();
    }

    @AfterAll
    public final void afterAllMethod() {
        log.info("After all test method.");
        afterAll();
    }

    /*
    @AfterAll
    public static void afterClass() {
        System.out.println("After test class: " + Dates.format(new Date(), Dates.DATEFULL_PATTERN));
    }
    */

    // --------------------------------------sub-class can override methods definition

    protected void beforeAll() {
        // default no-operation
    }

    protected void beforeEach() {
        // default no-operation
    }

    protected void afterEach() {
        // default no-operation
    }

    protected void afterAll() {
        // default no-operation
    }

}
