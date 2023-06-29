/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Async delayed executor
 *
 * @param <E> the element type
 * @author Ponfee
 */
public final class AsyncDelayedExecutor<E> extends Thread {

    private final static Logger LOG = LoggerFactory.getLogger(AsyncDelayedExecutor.class);

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
                .threadFactory(NamedThreadFactory.builder().prefix("async_delayed_worker").build())
                .rejectedHandler(ThreadPoolExecutors.CALLER_RUNS)
                .build();
        }
        this.asyncExecutor = executor;

        super.setName("async_delayed_executor-" + Integer.toHexString(hashCode()));
        super.setDaemon(false);
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
        Threads.stopThread(this, 0, 0, 1000);
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
                    asyncExecutor.submit(() -> processor.accept(data));
                } else {
                    processor.accept(data);
                }
            }
        }

        if (asyncExecutor != null) {
            // destroy the async executor
            ThreadPoolExecutors.shutdown(asyncExecutor, 1);
        }
    }

}
