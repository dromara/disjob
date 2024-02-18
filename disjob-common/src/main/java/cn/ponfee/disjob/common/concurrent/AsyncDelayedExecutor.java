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
import java.util.concurrent.atomic.AtomicBoolean;
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

    private final Consumer<E> processor;            // 数据处理器
    private final ThreadPoolExecutor asyncExecutor; // 异步执行器

    private final DelayQueue<DelayedData<E>> queue = new DelayQueue<>();
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    public AsyncDelayedExecutor(Consumer<E> processor) {
        this(1, processor);
    }

    /**
     * @param maximumPoolSize the maximumPoolSize
     * @param processor       the data processor
     */
    public AsyncDelayedExecutor(int maximumPoolSize,
                                Consumer<E> processor) {
        this.processor = processor;

        ThreadPoolExecutor executor = null;
        if (maximumPoolSize > 1) {
            executor = ThreadPoolExecutors.builder()
                .corePoolSize(1)
                .maximumPoolSize(maximumPoolSize)
                .workQueue(new SynchronousQueue<>())
                .keepAliveTimeSeconds(300)
                .threadFactory(NamedThreadFactory.builder().prefix("async_delayed_worker").uncaughtExceptionHandler(LOG).build())
                .rejectedHandler(ThreadPoolExecutors.CALLER_RUNS)
                .build();
        }
        this.asyncExecutor = executor;

        super.setName("async_delayed_executor-" + Integer.toHexString(hashCode()));
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
        if (stopped.get()) {
            return false;
        }
        return queue.offer(delayedData);
    }

    public boolean toStop() {
        return stopped.compareAndSet(false, true);
    }

    public void doStop() {
        toStop();
        Threads.stopThread(this, 1000);
    }

    @Override
    public void run() {
        while (!stopped.get()) {
            if (super.isInterrupted()) {
                LOG.error("Async delayed thread interrupted.");
                break;
            }
            DelayedData<E> delayed;
            try {
                delayed = queue.poll(3000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                LOG.error("Delayed queue pool interrupted.", e);
                toStop();
                Thread.currentThread().interrupt();
                break;
            }

            if (delayed != null) {
                E data = delayed.getData();
                if (asyncExecutor != null) {
                    asyncExecutor.submit(ThrowingRunnable.toCaught(() -> processor.accept(data)));
                } else {
                    ThrowingRunnable.doCaught(() -> processor.accept(data));
                }
            }
        }

        if (asyncExecutor != null) {
            // destroy the async executor
            ThreadPoolExecutors.shutdown(asyncExecutor, 1);
        }
    }

}
