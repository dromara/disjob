package com.ruoyi.common.config.thread;

import com.ruoyi.common.utils.Threads;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 *
 * @author ruoyi
 **/
@Configuration
public class ThreadPoolConfig {

    /**
     * 核心线程数
     */
    private final int corePoolSize = 10;

    public static final String SPRING_BEAN_NAME_SCHEDULED_EXECUTOR_SERVICE = "ruoyi.scheduledExecutorService";

    /**
     * 执行周期性或定时任务
     */
    @Bean(name = SPRING_BEAN_NAME_SCHEDULED_EXECUTOR_SERVICE)
    public ScheduledExecutorService scheduledExecutorService() {
        return new ScheduledThreadPoolExecutor(
            corePoolSize,
            new BasicThreadFactory.Builder().namingPattern("schedule-pool-%d").daemon(true).build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
        ) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                Threads.printException(r, t);
            }
        };
    }

}
