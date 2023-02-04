/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.base;

import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Immutable hash map & list structure
 *
 * @param <K> the key type
 * @param <V> the val type
 * @author Ponfee
 */
public class ImmutableHashList<K extends Comparable<K>, V> {
    private static final ImmutableHashList EMPTY = new ImmutableHashList<>();

    private final Function<V, K> mapper;
    private final Set<K> keys;
    private final List<V> values;

    private ImmutableHashList() {
        this.mapper = v -> null;
        this.keys = Collections.emptySet();
        this.values = Collections.emptyList();
    }

    private ImmutableHashList(List<V> values, Function<V, K> mapper) {
        // sort
        values.sort(Comparator.comparing(mapper));

        this.mapper = mapper;
        this.keys = values.stream().map(mapper).collect(Collectors.toSet());
        this.values = Collections.unmodifiableList(values);
    }

    public final List<V> values() {
        return values;
    }

    public final boolean contains(V value) {
        return keys.contains(mapper.apply(value));
    }

    public final boolean isEmpty() {
        return values.isEmpty();
    }

    public static <K extends Comparable<K>, V> ImmutableHashList<K, V> of(List<V> values, Function<V, K> mapper) {
        return CollectionUtils.isEmpty(values) ? EMPTY : new ImmutableHashList<>(values, mapper);
    }

    public static <K extends Comparable<K>, V> ImmutableHashList<K, V> empty() {
        return EMPTY;
    }

}
