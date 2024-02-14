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
