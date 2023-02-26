/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor;

import cn.ponfee.scheduler.common.date.Dates;
import cn.ponfee.scheduler.common.util.GenericUtils;
import cn.ponfee.scheduler.core.base.WorkerService;
import cn.ponfee.scheduler.supervisor.base.SpringTestCollector;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <pre>
 * Spring boot test base class
 *
 * 1、TestInstance：可以在非static方法上加@BeforeAll/@AfterAll注解
 *   LifeCycle.PER_METHOD(default)：每个测试方法执行前创建新的测试类实例
 *   Lifecycle.PER_CLASS：整个测试的过程中之创建一个测试类的实例
 *
 * 2、@MockitoSettings(strictness = Strictness.STRICT_STUBS)
 *   等同于：@ExtendWith(MockitoExtension.class)
 * </pre>
 *
 * @param <T> bean type
 * @author Ponfee
 */

/*
@org.junit.runner.RunWith(org.springframework.test.context.junit4.SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
*/

// 测试类(方法)要使用“@org.junit.jupiter.api.Test”来注解
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    classes = SpringBootTestApplication.class
)
//@ContextConfiguration(classes = { XXX.class })
//@ActiveProfiles({"DEV"})
public abstract class SpringBootTestBase<T> {

    // Only reset mock bean which is defined on SpringBootTestBase
    private static final List<Field> MOCK_BEAN_FIELDS = FieldUtils.getAllFieldsList(SpringBootTestBase.class)
        .stream()
        .filter(e -> !Modifier.isStatic(e.getModifiers()))
        .filter(e -> e.isAnnotationPresent(MockBean.class))
        .peek(e -> {
            if (!Modifier.isProtected(e.getModifiers())) {
                throw new AssertionError("Mock bean must protected: " + e.toGenericString());
            }
        })
        .collect(Collectors.toList());

    protected final Logger log = LoggerFactory.getLogger(getClass());

    // --------------------------------------mock bean definition

    @MockBean
    protected WorkerService workerService;

    // --------------------------------------member fields definition

    private final String beanName;

    protected T bean;

    @Resource
    protected ApplicationContext applicationContext;

    public SpringBootTestBase() {
        this(null);
    }

    public SpringBootTestBase(String beanName) {
        this.beanName = beanName;
    }

    // --------------------------------------annotated junit methods definition

    @BeforeAll
    public static void beforeClass() {
        System.out.println("before test class: " + Dates.format(new Date()));
    }

    @BeforeAll
    public final void beforeAll0() {
        log.info("before test all");
        beforeAll();
    }

    @BeforeEach
    public final void beforeEach0() {
        log.info("before test each");
        Class<T> type = GenericUtils.getActualTypeArgument(getClass(), 0);
        if (!Arrays.asList(Void.class, Object.class).contains(type)) {
            this.bean = StringUtils.isBlank(beanName)
                ? applicationContext.getBean(type)
                : applicationContext.getBean(beanName, type);
        }
        SpringTestCollector.collect(applicationContext, getClass());

        initMock();
        beforeEach();
    }

    @AfterEach
    public final void afterEach0() {
        log.info("after test each");
        afterEach();
        resetMock();
    }

    @AfterAll
    public final void afterAll0() {
        log.info("after test all");
        afterAll();
    }

    @AfterAll
    public static void afterClass() {
        System.out.println("after test class: " + Dates.format(new Date()));
    }

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

    // --------------------------------------private methods definition

    private void initMock() {
    }

    private void resetMock() {
        for (Field field : MOCK_BEAN_FIELDS) {
            try {
                Object mockBean = field.get(this);
                if (mockBean == null) {
                    log.error("Mock bean is null: " + field.toGenericString());
                } else {
                    Mockito.reset(mockBean);
                }
            } catch (Exception ex) {
                log.error("Mock bean reset error: " + field.toGenericString(), ex);
            }
        }
    }

}
