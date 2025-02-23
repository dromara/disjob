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

package cn.ponfee.disjob.common.concurrent;

import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * <pre>
 * Async delayed executor
 *
 * 延时任务方案：
 *   1、DelayQueue#take
 *   2、Timer#schedule
 *   3、ScheduledThreadPoolExecutor#schedule
 *   4、Netty: HashedWheelTimer#newTimeout
 *   5、RocketMQ: Message#setDelayTimeLevel
 *   6、RabbitMQ: x-dead-letter-exchange
 *   7、Redisson: RDelayedQueue
 * </pre>
 *
 * @param <E> the element type
 * @author Ponfee
 */
public final class AsyncDelayedExecutor<E> extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncDelayedExecutor.class);

    /**
     * 数据处理器
     */
    private final Consumer<E> dataProcessor;

    /**
     * 异步执行器
     */
    private final ThreadPoolExecutor asyncExecutor;

    private final DelayQueue<DelayedData<E>> queue = new DelayQueue<>();

    private final TripState state = TripState.createStarted();

    public AsyncDelayedExecutor(Consumer<E> dataProcessor) {
        this(1, dataProcessor);
    }

    /**
     * @param maximumPoolSize the maximumPoolSize
     * @param dataProcessor   the data processor
     */
    public AsyncDelayedExecutor(int maximumPoolSize, Consumer<E> dataProcessor) {
        this.dataProcessor = dataProcessor;

        ThreadPoolExecutor executor = null;
        if (maximumPoolSize > 1) {
            executor = ThreadPoolExecutors.builder()
                .corePoolSize(1)
                .maximumPoolSize(maximumPoolSize)
                .workQueue(new SynchronousQueue<>())
                .keepAliveTimeSeconds(300)
                .rejectedHandler(ThreadPoolExecutors.CALLER_RUNS)
                .threadFactory(NamedThreadFactory.builder().prefix("async_delayed_executor").daemon(true).uncaughtExceptionHandler(LOG).build())
                .build();
        }
        this.asyncExecutor = executor;

        super.setName("async_delayed_boss-" + Integer.toHexString(hashCode()));
        super.setDaemon(false);
        super.setUncaughtExceptionHandler(new LoggedUncaughtExceptionHandler(LOG));
        super.start();
    }

    /**
     * Puts an element to queue
     *
     * @param delayedData the delayed data
     */
    public boolean put(DelayedData<E> delayedData) {
        if (state.isStopped()) {
            return false;
        }
        return queue.offer(delayedData);
    }

    public boolean toStop() {
        return state.stop();
    }

    public void doStop() {
        toStop();
        Threads.stopThread(this, 2000);
    }

    @Override
    public void run() {
        while (state.isRunning()) {
            if (super.isInterrupted()) {
                LOG.error("Async delayed thread interrupted.");
                break;
            }
            DelayedData<E> delayed;
            try {
                delayed = queue.poll(2000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                LOG.warn("Delayed queue pool interrupted: {}", e.getMessage());
                Thread.currentThread().interrupt();
                break;
            }

            if (delayed != null) {
                E data = delayed.getData();
                if (asyncExecutor != null) {
                    asyncExecutor.submit(ThrowingRunnable.toCaught(() -> dataProcessor.accept(data)));
                } else {
                    ThrowingRunnable.doCaught(() -> dataProcessor.accept(data));
                }
            }
        }

        toStop();
        if (asyncExecutor != null) {
            // destroy the async executor
            ThreadPoolExecutors.shutdown(asyncExecutor, 1);
        }
    }

}
