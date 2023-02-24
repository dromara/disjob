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
import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.supervisor.configuration.EnableSupervisor;
import cn.ponfee.scheduler.test.EmbeddedMysqlAndRedisServer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Spring boot test base class
 *
 * @param <T> bean type
 * @author Ponfee
 */
/*
@org.junit.runner.RunWith(org.springframework.test.context.junit4.SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
*/

// 测试类(方法)要使用“@org.junit.jupiter.api.Test”来注解
@ExtendWith({MockitoExtension.class, SpringExtension.class})
//@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    classes = SpringBootTestBase.Application.class
)
//@ContextConfiguration(classes = { XXX.class })
//@ActiveProfiles({"DEV"})
public abstract class SpringBootTestBase<T> {

    // ----------------------------------------------------------static definitions

    private static final Class<?>[] EXCLUDE_CLASSES = {Void.class, Object.class};

    private static final ConcurrentMap<ApplicationContext, Set<Class<?>>> TEST_CLASSES_MAP = new ConcurrentHashMap<>();

    static {
        System.setProperty("app.name", "scheduler-supervisor-test");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            StringBuilder printer = new StringBuilder();
            printer.append("\n\n");
            printer.append("/*=================================Spring container & Test case=================================*\\");
            TEST_CLASSES_MAP.forEach((spring, classes) -> {
                printer.append("\n");
                printer.append(spring + ": \n");
                printer.append(classes.stream().map(e -> "---- " + e.getName()).collect(Collectors.joining("\n")));
                printer.append("\n");
            });
            printer.append("\\*=================================Spring container & Test case=================================*/");
            printer.append("\n\n");
            System.out.println(printer);
        }));
    }

    @EnableSupervisor
    @SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
    public static class Application {
        public static void main(String[] args) {
            SpringApplication.run(Application.class, args);
        }
    }

    private static EmbeddedMysqlAndRedisServer embeddedMysqlAndRedisServer;

    // ----------------------------------------------------------member definitions

    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private JdbcTemplate jdbcTemplate;

    private T bean;
    private final String beanName;

    public SpringBootTestBase() {
        this(null);
    }

    public SpringBootTestBase(String beanName) {
        this.beanName = beanName;
    }

    protected void initialize() {
        // do nothing
    }

    protected void destroy() {
        // do nothing
    }

    protected final ApplicationContext applicationContext() {
        return applicationContext;
    }

    protected final void executeSql(String sql) {
        jdbcTemplate.execute(sql);
    }

    protected final T bean() {
        return bean;
    }

    @BeforeAll
    public synchronized static void beforeAll() {
        System.out.println("------------------------SpringBootTestBase#beforeAll: " + Dates.format(new Date()));
        System.setProperty(JobConstants.SPRING_WEB_SERVER_PORT, "8080");
        if (embeddedMysqlAndRedisServer == null) {
            embeddedMysqlAndRedisServer = EmbeddedMysqlAndRedisServer.starter().start();
        }
    }

    @BeforeEach
    public final void beforeEach() {
        Class<T> type = GenericUtils.getActualTypeArgument(getClass(), 0);
        if (!ArrayUtils.contains(EXCLUDE_CLASSES, type)) {
            bean = StringUtils.isBlank(beanName)
                ? applicationContext.getBean(type)
                : applicationContext.getBean(beanName, type);
        }
        TEST_CLASSES_MAP.computeIfAbsent(applicationContext, k -> ConcurrentHashMap.newKeySet()).add(getClass());
        initialize();
    }

    @AfterEach
    public final void afterEach() {
        destroy();
    }

    @AfterAll
    public synchronized static void afterAll() {
        System.out.println("------------------------SpringBootTestBase#afterAll: " + Dates.format(new Date()));
    }

}
