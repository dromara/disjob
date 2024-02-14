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

package cn.ponfee.disjob.common.collect;

import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

/**
 * Delegated int spliterator
 *
 * @param <T> element type
 * @author Ponfee
 */
public class DelegatedIntSpliterator<T> implements Spliterator<T> {

    private static final int CHARACTERISTICS = Spliterator.ORDERED
                                             | Spliterator.SIZED
                                             | Spliterator.SUBSIZED
                                             | Spliterator.IMMUTABLE;

    private final OfInt delegate;
    private final IntFunction<? extends T> mapper;

    public DelegatedIntSpliterator(int startInclusive, int endExclusive, IntFunction<? extends T> mapper) {
        this.delegate = IntStream.range(startInclusive, endExclusive).spliterator();
        this.mapper = mapper;
    }

    public DelegatedIntSpliterator(OfInt delegate, IntFunction<? extends T> mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        return delegate.tryAdvance((IntConsumer) i -> action.accept(mapper.apply(i)));
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
        delegate.forEachRemaining((IntConsumer) i -> action.accept(mapper.apply(i)));
    }

    @Override
    public Spliterator<T> trySplit() {
        OfInt split = delegate.trySplit();
        return (split == null) ? null : new DelegatedIntSpliterator<>(split, mapper);
    }

    @Override
    public long estimateSize() {
        return delegate.estimateSize();
    }

    @Override
    public int characteristics() {
        return CHARACTERISTICS;
    }

    @Override
    public Comparator<? super T> getComparator() {
        // inner elements unsupported sortable
        throw new IllegalStateException();
    }

}
