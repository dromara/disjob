/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.concurrent;

import com.google.common.base.Stopwatch;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Multi Thread executor
 * <p> {@code Thread#stop()} will occur "java.lang.ThreadDeath: null" if try...catch wrapped in Throwable
 *
 * @author Ponfee
 */
public class MultithreadExecutors {

    private static final Logger LOG = LoggerFactory.getLogger(MultithreadExecutors.class);

    /**
     * Run async, action the T collection
     *
     * @param coll     the T collection
     * @param action   the T action
     * @param executor thread executor service
     */
    public static <T> void execute(Collection<T> coll, Consumer<T> action, Executor executor) {
        Stopwatch watch = Stopwatch.createStarted();
        coll.stream()
            .map(e -> CompletableFuture.runAsync(() -> action.accept(e), executor))
            .collect(Collectors.toList())
            .forEach(CompletableFuture::join);
        LOG.info("multi thread run async duration: {}", watch.stop());
    }

    /**
     * Call async, mapped T to U
     *
     * @param coll     the T collection
     * @param mapper   the mapper of T to U
     * @param executor thread executor service
     * @return the U collection
     */
    public static <T, U> List<U> execute(Collection<T> coll, Function<T, U> mapper, Executor executor) {
        Stopwatch watch = Stopwatch.createStarted();
        List<U> result = coll.stream()
            .map(e -> CompletableFuture.supplyAsync(() -> mapper.apply(e), executor))
            .collect(Collectors.toList())
            .stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        LOG.info("multi thread call async duration: {}", watch.stop());
        return result;
    }

    /**
     * 根据数据（任务）数量来判断是否主线程执行还是提交到线程池执行
     *
     * @param data              the data
     * @param action            the action
     * @param dataSizeThreshold the dataSizeThreshold
     * @param executor          the executor
     * @param <T>               data element type
     * @param <R>               result element type
     * @return list for action result
     */
    public static <T, R> List<R> execute(Collection<T> data, Function<T, R> action,
                                         int dataSizeThreshold, Executor executor) {
        if (CollectionUtils.isEmpty(data)) {
            return Collections.emptyList();
        }
        if (dataSizeThreshold < 1 || data.size() < dataSizeThreshold) {
            return data.stream().map(action).collect(Collectors.toList());
        }

        CompletionService<R> service = new ExecutorCompletionService<>(executor);
        data.forEach(e -> service.submit(() -> action.apply(e)));
        return join(service, data.size());
    }

    /**
     * 根据数据（任务）数量来判断是否主线程执行还是提交到线程池执行
     *
     * @param data              the data
     * @param action            the action
     * @param dataSizeThreshold the dataSizeThreshold
     * @param executor          the executor
     * @param <T>               data element type
     */
    public static <T> void execute(Collection<T> data, Consumer<T> action,
                                   int dataSizeThreshold, Executor executor) {
        if (CollectionUtils.isEmpty(data)) {
            return;
        }
        if (dataSizeThreshold < 1 || data.size() < dataSizeThreshold) {
            data.forEach(action);
            return;
        }

        CompletionService<Void> service = new ExecutorCompletionService<>(executor);
        data.forEach(e -> service.submit(() -> action.accept(e), null));
        joinDiscard(service, data.size());
    }

    // -----------------------------------------------------------------join

    public static <T> List<T> join(CompletionService<T> service, int count) {
        List<T> result = new ArrayList<>(count);
        join(service, count, result::add);
        return result;
    }

    public static <T> void joinDiscard(CompletionService<T> service, int count) {
        join(service, count, t -> { });
    }

    public static <T> void join(CompletionService<T> service, int count, Consumer<T> accept) {
        try {
            while (count-- > 0) {
                // block until a task done
                Future<T> future = service.take();
                accept.accept(future.get());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

}
