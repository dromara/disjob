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
     * @param coll              the collection
     * @param action            the action
     * @param executor          the executor
     * @param dataSizeThreshold the dataSizeThreshold
     * @param <T>               the collection element type
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
     * Convert collection element data
     *
     * @param coll              the collection
     * @param mapper            the mapper
     * @param executor          the executor
     * @param dataSizeThreshold the executor
     * @param <T>               the source collection element type
     * @param <U>               the target collection element type
     * @return target collection
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
