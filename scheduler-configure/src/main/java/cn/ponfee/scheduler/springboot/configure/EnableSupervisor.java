package cn.ponfee.scheduler.springboot.configure;

import cn.ponfee.scheduler.common.util.ClassUtils;
import cn.ponfee.scheduler.common.util.Networks;
import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.Supervisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.annotation.*;

/**
 * Enable supervisor role
 *
 * @Order、Order接口、@AutoConfigureBefore、@AutoConfigureAfter、@AutoConfigureOrder的顺序：
 *   1）用户自定义的类之间的顺序是按照文件的目录结构从上到下排序且无法干预，在这里这些方式都是无效的；
 *   2）自动装配的类之间可以使用这五种方式去改变加载的顺序（用户自定义的类 排在 EnableAutoConfiguration自动配置加载的类 的前面）；
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(EnableSupervisor.SupervisorConfiguration.class)
public @interface EnableSupervisor {

    @ConditionalOnClass({Supervisor.class})
    @ConditionalOnProperty(JobConstants.SPRING_WEB_SERVER_PORT)
    @AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
    class SupervisorConfiguration {
        @Bean(JobConstants.SPRING_BEAN_NAME_CURRENT_SUPERVISOR)
        @Order(Ordered.HIGHEST_PRECEDENCE)
        @ConditionalOnMissingBean
        public Supervisor currentSupervisor(@Value("${" + JobConstants.SPRING_WEB_SERVER_PORT + "}") int port) {
            Supervisor currentSupervisor = new Supervisor(Networks.getHostIp(), port);
            // inject current supervisor: Supervisor.class.getDeclaredClasses()[0]
            try {
                ClassUtils.invoke(Class.forName(Supervisor.class.getName() + "$Current"), "set", new Object[]{currentSupervisor});
            } catch (ClassNotFoundException e) {
                // cannot happen
                throw new AssertionError("Setting as current supervisor occur error.", e);
            }
            return currentSupervisor;
        }
    }

}
