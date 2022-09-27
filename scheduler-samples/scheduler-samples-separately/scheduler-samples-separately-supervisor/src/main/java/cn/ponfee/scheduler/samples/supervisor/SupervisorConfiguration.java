package cn.ponfee.scheduler.samples.supervisor;

import cn.ponfee.scheduler.common.base.Constants;
import cn.ponfee.scheduler.common.base.IdGenerator;
import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.common.lock.DoInDatabaseLocked;
import cn.ponfee.scheduler.common.spring.SpringContextHolder;
import cn.ponfee.scheduler.common.util.ClassUtils;
import cn.ponfee.scheduler.common.util.Networks;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.dispatch.TaskDispatcher;
import cn.ponfee.scheduler.dispatch.redis.RedisTaskDispatcher;
import cn.ponfee.scheduler.registry.Discovery;
import cn.ponfee.scheduler.registry.ServerRegistry;
import cn.ponfee.scheduler.registry.redis.RedisSupervisorRegistry;
import cn.ponfee.scheduler.supervisor.ScanJobHeartbeatThread;
import cn.ponfee.scheduler.supervisor.ScanTrackHeartbeatThread;
import cn.ponfee.scheduler.supervisor.base.SupervisorConstants;
import cn.ponfee.scheduler.supervisor.manager.JobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;

import javax.annotation.PreDestroy;

import static cn.ponfee.scheduler.supervisor.base.SupervisorConstants.*;
import static cn.ponfee.scheduler.supervisor.dao.JobDataSourceConfig.DB_NAME;

/**
 * Job supervisor configuration.
 *
 * @author Ponfee
 */
@Configuration
public class SupervisorConfiguration {

    @Bean(SupervisorConstants.SPRING_BEAN_NAME_CURRENT_SUPERVISOR)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public Supervisor currentSupervisor(@Value("${server.port}") int port) throws ClassNotFoundException {
        Supervisor supervisor = new Supervisor(Networks.getHostIp(), port);
        // inject current supervisor
        ClassUtils.invoke(Class.forName(Supervisor.class.getName() + "$Current"), "set", new Object[]{supervisor});
        return supervisor;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public IdGenerator idGenerator() {
        return new IdGenerator(1);
    }

    @Bean
    public SpringContextHolder springContextHolder() {
        return new SpringContextHolder();
    }

    /**
     * RedisAutoConfiguration has auto-configured two redis template objects.
     * <p>RedisTemplate<Object, Object> redisTemplate
     * <p>StringRedisTemplate stringRedisTemplate
     *
     * @param currentSupervisor   currentSupervisor
     * @param stringRedisTemplate the auto-configured redis template by spring container
     * @return ServerRegistry<Supervisor, Worker>
     * @see org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
     */
    @Bean
    public ServerRegistry<Supervisor, Worker> supervisorRegistry(Supervisor currentSupervisor,
                                                                 StringRedisTemplate stringRedisTemplate) {
        RedisSupervisorRegistry supervisorRegistry = new RedisSupervisorRegistry(stringRedisTemplate);
        supervisorRegistry.register(currentSupervisor);
        return supervisorRegistry;
    }

    @Bean
    public TaskDispatcher taskDispatcher(RedisTemplate<String, String> redisTemplate,
                                         Discovery<Worker> discoveryWorker,
                                         @Nullable TimingWheel<ExecuteParam> timingWheel) {
        return new RedisTaskDispatcher(redisTemplate, discoveryWorker, timingWheel);
    }

    @DependsOn(SupervisorConstants.SPRING_BEAN_NAME_CURRENT_SUPERVISOR)
    @Bean
    public ScanJobHeartbeatThread schedJobHeartbeatThread(@Value("${" + DISTRIBUTED_SCHEDULER_SUPERVISOR + ".job.heartbeat-interval-seconds:3}") int heartbeatIntervalSeconds,
                                                          @Qualifier(DB_NAME + Constants.JDBC_TEMPLATE_SUFFIX) JdbcTemplate jdbcTemplate,
                                                          JobManager jobManager,
                                                          IdGenerator idGenerator) {
        ScanJobHeartbeatThread scanJobHeartbeatThread = new ScanJobHeartbeatThread(
            heartbeatIntervalSeconds,
            new DoInDatabaseLocked(jdbcTemplate, LOCK_SQL_SCAN_JOB),
            jobManager,
            idGenerator
        );
        scanJobHeartbeatThread.start();
        return scanJobHeartbeatThread;
    }

    @DependsOn(SupervisorConstants.SPRING_BEAN_NAME_CURRENT_SUPERVISOR)
    @Bean
    public ScanTrackHeartbeatThread scanTrackHeartbeatThread(@Value("${" + DISTRIBUTED_SCHEDULER_SUPERVISOR + ".track.heartbeat-interval-seconds:3}") int heartbeatIntervalSeconds,
                                                             JobManager jobManager,
                                                             @Qualifier(DB_NAME + Constants.JDBC_TEMPLATE_SUFFIX) JdbcTemplate jdbcTemplate) {
        ScanTrackHeartbeatThread scanTrackHeartbeatThread = new ScanTrackHeartbeatThread(
            heartbeatIntervalSeconds,
            new DoInDatabaseLocked(jdbcTemplate, LOCK_SQL_SCAN_TRACK),
            jobManager
        );
        scanTrackHeartbeatThread.start();
        return scanTrackHeartbeatThread;
    }

    /**
     * Destroy job supervisor thread heartbeat.
     */
    @Configuration
    //@ConditionalOnBean({SchedJobHeartbeatThread.class, SchedTrackHeartbeatThread.class})
    private static class SupervisorDisposableBean {
        private static final Logger LOG = LoggerFactory.getLogger(SupervisorDisposableBean.class);

        private final ServerRegistry<Supervisor, Worker> supervisorRegistry;
        private final ScanJobHeartbeatThread scanJobHeartbeatThread;
        private final ScanTrackHeartbeatThread scanTrackHeartbeatThread;

        public SupervisorDisposableBean(ServerRegistry<Supervisor, Worker> supervisorRegistry,
                                        ScanJobHeartbeatThread scanJobHeartbeatThread,
                                        ScanTrackHeartbeatThread scanTrackHeartbeatThread) {
            this.supervisorRegistry = supervisorRegistry;
            this.scanJobHeartbeatThread = scanJobHeartbeatThread;
            this.scanTrackHeartbeatThread = scanTrackHeartbeatThread;
        }

        @PreDestroy
        public void preDestroy() {
            LOG.info("Job supervisor destroy begin...");

            supervisorRegistry.close();

            // 1、to stop the scan
            scanJobHeartbeatThread.toStop();
            scanTrackHeartbeatThread.toStop();

            // 2、do stop the scan
            try {
                scanJobHeartbeatThread.doStop(3000);
            } catch (Exception e) {
                LOG.error("Destroy sched job heartbeat thread occur error", e);
            }
            try {
                scanTrackHeartbeatThread.doStop(3000);
            } catch (Exception e) {
                LOG.error("Destroy sched track heartbeat thread occur error", e);
            }

            LOG.info("Job supervisor destroy end.");
        }
    }

}
