package cn.ponfee.scheduler.common.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.DelayQueue;
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
            executor = ThreadPoolExecutors.create(
                1, maximumPoolSize, 300, 0,
                "async_delayed_worker",
                ThreadPoolExecutors.ALWAYS_CALLER_RUNS
            );
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

    public void doStop() {
        if (stopped.compareAndSet(false, true)) {
            MultithreadExecutors.stopThread(this, 0, 0, 1000);
        }
    }

    @Override
    public void run() {
        while (!stopped.get()) {
            DelayedData<E> delayed;
            try {
                delayed = queue.poll(3000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                LOG.error("Delayed queue pool occur interrupted.", e);
                stopped.compareAndSet(false, true);
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
            ThreadPoolExecutors.shutdown(asyncExecutor);
        }
    }

}
