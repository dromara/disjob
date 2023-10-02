/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

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
