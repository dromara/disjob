/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.concurrent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Multi Thread executor
 *
 * @author Ponfee
 */
public class MultithreadExecutors {

    public static <T> void run(Collection<T> coll, Consumer<T> action, Executor executor) {
        run(coll, action, executor, 2);
    }

    /**
     * Run async, action the T collection
     *
     * @param coll     the T collection
     * @param action   the T action
     * @param executor thread executor service
     */
    public static <T> void run(Collection<T> coll, Consumer<T> action, Executor executor, int dataSizeThreshold) {
        if (coll == null || coll.isEmpty()) {
            return;
        }
        if (dataSizeThreshold <= 0 || coll.size() < dataSizeThreshold) {
            coll.forEach(action);
            return;
        }
        coll.stream()
            .map(e -> CompletableFuture.runAsync(() -> action.accept(e), executor))
            .collect(Collectors.toList())
            .forEach(CompletableFuture::join);
    }

    public static <T, U> List<U> call(Collection<T> coll, Function<T, U> mapper, Executor executor) {
        return call(coll, mapper, executor, 2);
    }

    /**
     * Call async, mapped T to U
     *
     * @param coll     the T collection
     * @param mapper   the mapper of T to U
     * @param executor thread executor service
     * @return the U collection
     */
    public static <T, U> List<U> call(Collection<T> coll, Function<T, U> mapper, Executor executor, int dataSizeThreshold) {
        if (coll == null) {
            return null;
        }
        if (coll.isEmpty()) {
            return Collections.emptyList();
        }
        if (dataSizeThreshold <= 0 || coll.size() < dataSizeThreshold) {
            return coll.stream().map(mapper).collect(Collectors.toList());
        }
        return coll.stream()
            .map(e -> CompletableFuture.supplyAsync(() -> mapper.apply(e), executor))
            .collect(Collectors.toList())
            .stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
    }

}
