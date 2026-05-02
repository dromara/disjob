package cn.ponfee.disjob.worker.configuration;

import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.common.spring.SpringUtils;
import cn.ponfee.disjob.common.util.ClassUtils;
import cn.ponfee.disjob.common.util.UuidUtils;
import cn.ponfee.disjob.core.base.CoreUtils;
import cn.ponfee.disjob.core.worker.Worker;
import cn.ponfee.disjob.core.worker.WorkerRpcService;
import cn.ponfee.disjob.core.worker.dto.ExecuteTaskParam;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.worker.provider.WorkerRpcProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Worker configuration
 *
 * @author Ponfee
 */
@EnableConfigurationProperties(WorkerProperties.class)
class WorkerConfiguration {

    @Bean
    Worker.Local localWorker(WebServerApplicationContext webServerApplicationContext, WorkerProperties config) {
        config.check();
        String host = CoreUtils.getLocalHost();
        String workerToken = config.getWorkerToken();
        String supervisorToken = config.getSupervisorToken();
        String supervisorContextPath = config.getSupervisorContextPath();
        int port = SpringUtils.getWebServerPort(webServerApplicationContext);

        Object[] args = {config.getGroup(), UuidUtils.uuid32(), host, port, workerToken, supervisorToken, supervisorContextPath};
        try {
            return ClassUtils.invoke(Worker.Local.class, "create", args);
        } catch (Exception e) {
            // cannot happen
            throw new Error("Creates Worker.Local instance occur error.", e);
        }
    }

    @Bean
    WorkerRpcService workerRpcService(Worker.Local localWorker,
                                      TimingWheel<ExecuteTaskParam> timingWheel,
                                      WorkerRegistry workerRegistry) {
        return WorkerRpcProvider.create(localWorker, timingWheel, workerRegistry);
    }

}
