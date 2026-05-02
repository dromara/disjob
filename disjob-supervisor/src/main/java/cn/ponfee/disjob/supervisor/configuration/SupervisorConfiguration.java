package cn.ponfee.disjob.supervisor.configuration;

import cn.ponfee.disjob.common.spring.SpringUtils;
import cn.ponfee.disjob.common.util.ClassUtils;
import cn.ponfee.disjob.common.util.Strings;
import cn.ponfee.disjob.core.base.CoreUtils;
import cn.ponfee.disjob.core.supervisor.GroupInfoService;
import cn.ponfee.disjob.core.supervisor.Supervisor;
import cn.ponfee.disjob.supervisor.SupervisorStartup;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Supervisor configuration
 *
 * @author Ponfee
 */
@EnableConfigurationProperties(SupervisorProperties.class)
@ComponentScan(basePackageClasses = SupervisorStartup.class)
class SupervisorConfiguration {

    @Bean
    Supervisor.Local localSupervisor(WebServerApplicationContext webServerApplicationContext,
                                     ServerProperties serverProperties,
                                     GroupInfoService groupInfoService) {
        int port = SpringUtils.getWebServerPort(webServerApplicationContext);
        String contextPath = Strings.trimPath(serverProperties.getServlet().getContextPath());
        Object[] args = {CoreUtils.getLocalHost(), port, contextPath, groupInfoService};
        try {
            // create local supervisor: Supervisor.class.getDeclaredClasses()[0]
            return ClassUtils.invoke(Supervisor.Local.class, "create", args);
        } catch (Exception e) {
            // cannot happen
            throw new Error("Creates Supervisor.Local instance occur error.", e);
        }
    }

}
