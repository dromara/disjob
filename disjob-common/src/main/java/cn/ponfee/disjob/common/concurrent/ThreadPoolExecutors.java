/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.concurrent;

import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.util.Numbers;
import cn.ponfee.disjob.common.util.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.concurrent.*;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;

/**
 * Thread pool executor utility
 *
 * @author Ponfee
 */
public final class ThreadPoolExecutors {

    private static final Logger LOG = LoggerFactory.getLogger(ThreadPoolExecutors.class);
    private static final String DISJOB_COMMON_POOL_SIZE = "disjob.common.pool.size";

    private static volatile ThreadPoolExecutor          commonThreadPool;
    private static volatile ScheduledThreadPoolExecutor commonScheduledPool;

    /**
     * max #workers - 1
     */
    public static final int MAX_CAP = 0x7FFF;

    // ----------------------------------------------------------build-in rejected policy
    /**
     * Throw RejectedExecutionException
     */
    public static final RejectedExecutionHandler ABORT = new AbortPolicy();

    /**
     * Discard the current task
     */
    public static final RejectedExecutionHandler DISCARD = new DiscardPolicy();

    /**
     * Caller thread run the current task
     */
    public static final RejectedExecutionHandler CALLER_RUNS = new CallerRunsPolicy();

    /**
     * Discard oldest and execute the new
     */
    public static final RejectedExecutionHandler DISCARD_OLDEST = new DiscardOldestPolicy();

    /**
     * Caller thread run the oldest task
     */
    public static final RejectedExecutionHandler CALLER_RUNS_OLDEST = (currentTask, executor) -> {
        if (executor.isShutdown()) {
            return;
        }
        BlockingQueue<Runnable> workQueue = executor.getQueue();
        Runnable oldestTask = workQueue.poll();
        boolean state = workQueue.offer(currentTask);
        if (oldestTask != null) {
            oldestTask.run();
        }
        if (!state) {
            executor.execute(currentTask);
        }
    };

    /**
     * Synchronized put the current task to queue
     */
    public static final RejectedExecutionHandler CALLER_BLOCKS = (task, executor) -> {
        if (executor.isShutdown()) {
            return;
        }
        try {
            executor.getQueue().put(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ExceptionUtils.rethrow(e);
        }
    };

    /**
     * Anyway always run, ignore the thread pool is whether shutdown
     */
    public static final RejectedExecutionHandler CALLER_RUNS_ANYWAY = (task, executor) -> task.run();

    /**
     * Common ThreadPoolExecutor, IO bound / IO intensive
     *
     * @return ThreadPoolExecutor
     */
    public static ThreadPoolExecutor commonThreadPool() {
        if (commonThreadPool == null) {
            synchronized (ThreadPoolExecutors.class) {
                if (commonThreadPool == null) {
                    commonThreadPool = makeThreadPoolExecutor();
                }
            }
        }
        return commonThreadPool;
    }

    /**
     * Common ScheduledThreadPoolExecutor
     *
     * @return ScheduledThreadPoolExecutor
     */
    public static ScheduledThreadPoolExecutor commonScheduledPool() {
        if (commonScheduledPool == null) {
            synchronized (ThreadPoolExecutors.class) {
                if (commonScheduledPool == null) {
                    commonScheduledPool = makeCommonScheduledThreadPoolExecutor();
                }
            }
        }
        return commonScheduledPool;
    }

    // ----------------------------------------------------------builder

    public static Builder builder() {
        return new Builder();
    }

    public enum PrestartCoreThreadType {
        /**
         * Not start core thread
         */
        NONE {
            @Override
            public void prestart(ThreadPoolExecutor executor) {
                // do nothing
            }
        },
        /**
         * Starts a core thread
         */
        ONE {
            @Override
            public void prestart(ThreadPoolExecutor executor) {
                executor.prestartCoreThread();
            }
        },
        /**
         * Starts all core threads
         */
        ALL {
            @Override
            public void prestart(ThreadPoolExecutor executor) {
                executor.prestartAllCoreThreads();
            }
        };

        public abstract void prestart(ThreadPoolExecutor executor);
    }

    public static class Builder {
        private int corePoolSize;
        private int maximumPoolSize;
        private BlockingQueue<Runnable> workQueue;
        private long keepAliveTimeSeconds;
        private RejectedExecutionHandler rejectedHandler;
        private ThreadFactory threadFactory;
        private boolean allowCoreThreadTimeOut = true;
        private PrestartCoreThreadType prestartCoreThreadType = PrestartCoreThreadType.NONE;

        private Builder() {
        }

        public Builder corePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
            return this;
        }

        public Builder maximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
            return this;
        }

        public Builder workQueue(BlockingQueue<Runnable> workQueue) {
            this.workQueue = workQueue;
            return this;
        }

        public Builder keepAliveTimeSeconds(long keepAliveTimeSeconds) {
            this.keepAliveTimeSeconds = keepAliveTimeSeconds;
            return this;
        }

        public Builder rejectedHandler(RejectedExecutionHandler rejectedHandler) {
            this.rejectedHandler = rejectedHandler;
            return this;
        }

        public Builder threadFactory(ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
            return this;
        }

        public Builder allowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
            this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
            return this;
        }

        public Builder prestartCoreThreadType(PrestartCoreThreadType prestartCoreThreadType) {
            this.prestartCoreThreadType = prestartCoreThreadType;
            return this;
        }

        public ThreadPoolExecutor build() {
            Assert.isTrue(maximumPoolSize > 0, () -> String.format("Maximum pool size %d must greater than 0.", maximumPoolSize));
            Assert.isTrue(maximumPoolSize <= MAX_CAP, () -> String.format("Maximum pool size %d cannot greater than %d.", maximumPoolSize, MAX_CAP));
            Assert.isTrue(corePoolSize > 0, () -> String.format("Core pool size %d must greater than 0.", corePoolSize));
            Assert.isTrue(corePoolSize <= maximumPoolSize, () -> String.format("Core pool size %d cannot greater than maximum pool size %d.", corePoolSize, maximumPoolSize));
            Assert.notNull(workQueue, "Worker queue cannot be null.");

            // create ThreadPoolExecutor instance
            ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTimeSeconds,
                TimeUnit.SECONDS,
                workQueue,
                threadFactory != null ? threadFactory : Executors.defaultThreadFactory(),
                rejectedHandler != null ? rejectedHandler : CALLER_RUNS
            );

            threadPoolExecutor.allowCoreThreadTimeOut(allowCoreThreadTimeOut);
            prestartCoreThreadType.prestart(threadPoolExecutor);
            return threadPoolExecutor;
        }
    }

    // ----------------------------------------------------------shutdown

    /**
     * Shutdown the ExecutorService safe
     *
     * @param executorService the executorService
     * @return is safe shutdown
     */
    public static boolean shutdown(ExecutorService executorService) {
        try {
            executorService.shutdown();
            while (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                // do nothing
            }
            return true;
        } catch (Throwable t) {
            LOG.error("Shutdown ExecutorService occur error.", t);
            ThrowingRunnable.doCaught(executorService::shutdownNow);
            Threads.interruptIfNecessary(t);
            return false;
        }
    }

    /**
     * Shutdown the executorService max wait time
     *
     * @param executorService the executorService
     * @param awaitSeconds    await time seconds
     * @return {@code true} if safe terminate
     */
    public static boolean shutdown(ExecutorService executorService, int awaitSeconds) {
        boolean isSafeTerminated = false;
        boolean hasCallShutdownNow = false;
        try {
            executorService.shutdown();
            isSafeTerminated = executorService.awaitTermination(awaitSeconds, TimeUnit.SECONDS);
            if (!isSafeTerminated) {
                hasCallShutdownNow = true;
                executorService.shutdownNow();
            }
        } catch (Throwable t) {
            LOG.error("Shutdown ExecutorService occur error.", t);
            if (!hasCallShutdownNow) {
                ThrowingRunnable.doCaught(executorService::shutdownNow);
            }
            Threads.interruptIfNecessary(t);
        }
        return isSafeTerminated;
    }

    private static ThreadPoolExecutor makeThreadPoolExecutor() {
        int poolSize = Numbers.toInt(SystemUtils.getConfig(DISJOB_COMMON_POOL_SIZE), Runtime.getRuntime().availableProcessors() * 4);
        if (poolSize < 0 || poolSize > MAX_CAP) {
            LOG.warn("Invalid disjob common pool size config value: {}", poolSize);
            poolSize = Numbers.bound(poolSize, 1, MAX_CAP);
        }

        final ThreadPoolExecutor threadPool = ThreadPoolExecutors.builder()
            .corePoolSize(poolSize)
            .maximumPoolSize(poolSize)
            .workQueue(new ArrayBlockingQueue<>(poolSize * 10))
            .keepAliveTimeSeconds(600)
            .rejectedHandler(ThreadPoolExecutors.CALLER_RUNS)
            .threadFactory(NamedThreadFactory.builder().prefix("disjob-common-thread-pool").priority(Thread.MAX_PRIORITY).build())
            .build();

        Runtime.getRuntime().addShutdownHook(new Thread(threadPool::shutdown));
        return threadPool;
    }

    private static ScheduledThreadPoolExecutor makeCommonScheduledThreadPoolExecutor() {
        ScheduledThreadPoolExecutor scheduledPool = new ScheduledThreadPoolExecutor(
            1,
            NamedThreadFactory.builder().prefix("disjob-common-scheduled-pool").priority(Thread.MAX_PRIORITY).build(),
            CALLER_RUNS
        );
        Runtime.getRuntime().addShutdownHook(new Thread(scheduledPool::shutdown));
        return scheduledPool;
    }

}
