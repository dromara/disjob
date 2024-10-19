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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Immutable set & list structure
 *
 * @param <T> the element type
 * @author Ponfee
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ImmutableSetList<T extends Comparable<T>> {

    private static final ImmutableSetList EMPTY = new ImmutableSetList<>();

    private final Set<T> set;
    private final List<T> list;

    private ImmutableSetList() {
        this.set = Collections.emptySet();
        this.list = Collections.emptyList();
    }

    private ImmutableSetList(List<T> list) {
        this.list = list.stream().sorted().collect(ImmutableList.toImmutableList());
        this.set = ImmutableSet.copyOf(list);
    }

    public List<T> values() {
        return list;
    }

    public boolean contains(T value) {
        return set.contains(value);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public static <T extends Comparable<T>> ImmutableSetList<T> of(List<T> values) {
        return CollectionUtils.isEmpty(values) ? EMPTY : new ImmutableSetList<>(values);
    }

    public static <T extends Comparable<T>> ImmutableSetList<T> empty() {
        return EMPTY;
    }

}
