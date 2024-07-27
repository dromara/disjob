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

import cn.ponfee.disjob.common.util.Numbers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.lang.reflect.Array;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Collection utilities
 *
 * @author Ponfee
 */
public class Collects {

    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    public static <E> LinkedList<E> newLinkedList(E element) {
        LinkedList<E> list = new LinkedList<>();
        list.add(element);
        return list;
    }

    public static <T> Set<T> truncate(Set<T> set, int length) {
        if (CollectionUtils.isEmpty(set) || length <= 0 || set.size() <= length) {
            return set;
        }

        Set<T> result = new HashSet<>(length << 1);
        int i = 1;
        for (T t : set) {
            result.add(t);
            if (++i > length) {
                break;
            }
        }
        return result;
    }

    public static <T> List<T> duplicate(List<T> list) {
        return duplicate(list, Function.identity());
    }

    public static <R> Set<R> split(String str, String separator, Function<String, R> converter) {
        if (StringUtils.isEmpty(str)) {
            return Collections.emptySet();
        }
        return Arrays.stream(str.split(separator))
            .filter(StringUtils::isNotBlank)
            .map(e -> converter.apply(e.trim()))
            .collect(Collectors.toSet());
    }

    /**
     * Returns the duplicates elements for list
     *
     * @param list the list
     * @return a set of duplicates elements for list
     */
    public static <T, R> List<R> duplicate(List<T> list, Function<T, R> mapper) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }

        return list.stream()
            .map(mapper)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet()
            .stream()
            .filter(e -> e.getValue() > 1)
            .map(Entry::getKey)
            .collect(Collectors.toList());
    }

    public static <E> List<E> sorted(List<E> list, Comparator<? super E> comparator) {
        if (list == null || list.size() <= 1) {
            return list;
        }

        Class<? extends List> type = list.getClass();
        if (type == ArrayList.class || type == LinkedList.class) {
            list.sort(comparator);
            return list;
        }

        return list.stream().sorted(comparator).collect(Collectors.toList());
    }

    /**
     * Gets the first element from collection
     *
     * @param coll the coll
     * @param <T>  the coll element type
     * @return first element of coll
     */
    public static <T> T getFirst(Collection<T> coll) {
        if (coll == null || coll.isEmpty()) {
            return null;
        }
        if (coll instanceof Deque) {
            return ((Deque<T>) coll).getFirst();
        }
        if (coll instanceof SortedSet) {
            return ((SortedSet<T>) coll).first();
        }
        if (coll instanceof List) {
            return ((List<T>) coll).get(0);
        }
        return coll.iterator().next();
    }

    /**
     * Gets the last element from collection
     *
     * @param coll the coll
     * @param <T>  the coll element type
     * @return last element of coll
     */
    public static <T> T getLast(Collection<T> coll) {
        if (coll == null || coll.isEmpty()) {
            return null;
        }
        if (coll instanceof Deque) {
            return ((Deque<T>) coll).getLast();
        }
        if (coll instanceof SortedSet) {
            return ((SortedSet<T>) coll).last();
        }
        if (coll instanceof List) {
            return ((List<T>) coll).get(coll.size() - 1);
        }
        return coll.stream().reduce((a, b) -> b).orElse(null);
    }

    public static <T> T get(T[] array, int index) {
        if (array == null || index < 0 || index >= array.length) {
            return null;
        }
        return array[index];
    }

    public static <T> T get(List<T> list, int index) {
        if (list == null || index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    public static <T> void batchProcess(List<T> list, Consumer<List<T>> processor, int batchSize) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        if (list.size() <= batchSize) {
            processor.accept(list);
        } else {
            Lists.partition(list, batchSize).forEach(processor);
        }
    }

    /**
     * Returns consecutive sub array of an array,
     * each of the same size (the final list may be smaller).
     *
     * <pre>
     *  Collects.partition(new int[]{1,1,2,5,3}, 1)    ->  [1, 1, 2, 5, 3]
     *  Collects.partition(new int[]{1,1,2,5,3}, 3)    ->  [1, 1]; [2, 5]; [3]
     *  Collects.partition(new int[]{1,1,2,5,3}, 5)    ->  [1]; [1]; [2]; [5]; [3]
     *  Collects.partition(new int[]{1,1,2,5,3}, 6)    ->  [1]; [1]; [2]; [5]; [3]
     *  Collects.partition(new int[]{1,1,2,5,3}, 100)  ->  [1]; [1]; [2]; [5]; [3]
     * </pre>
     *
     * @param array the array
     * @param size  the size
     * @return a list of consecutive sub sets
     */
    public static List<int[]> partition(int[] array, int size) {
        Assert.isTrue(size > 0, "Size must be greater than 0.");
        if (array == null || array.length == 0) {
            return null;
        }
        size = Math.min(size, array.length);
        if (size == 1) {
            return Collections.singletonList(array);
        }

        List<int[]> result = new ArrayList<>(size);
        int pos = 0;
        for (int number : Numbers.slice(array.length, size)) {
            if (number == 0) {
                break;
            }
            result.add(Arrays.copyOfRange(array, pos, pos = pos + number));
        }
        return result;
    }

    public static <T> List<T> filter(List<T> list, Predicate<T> predicate) {
        if (list == null) {
            return null;
        }
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().filter(predicate).collect(Collectors.toList());
    }

    public static <T> T findAny(Collection<T> coll, Predicate<T> predicate) {
        if (CollectionUtils.isEmpty(coll)) {
            return null;
        }
        return coll.stream().filter(predicate).findAny().orElse(null);
    }

    public static <S, T> List<T> convert(Collection<S> source, Function<S, T> mapper) {
        if (source == null) {
            return null;
        }
        if (source.isEmpty()) {
            return Collections.emptyList();
        }
        return source.stream().map(mapper).collect(ImmutableList.toImmutableList());
    }

    public static <S, T> Set<T> convert(Set<S> source, Function<S, T> mapper) {
        if (source == null) {
            return null;
        }
        if (source.isEmpty()) {
            return Collections.emptySet();
        }
        return source.stream().map(mapper).collect(ImmutableSet.toImmutableSet());
    }

    @SafeVarargs
    public static <T> List<T> concat(List<T> list, T... array) {
        if (list == null) {
            return array == null ? Collections.emptyList() : Arrays.asList(array);
        }
        if (array == null || array.length == 0) {
            return list;
        }
        List<T> result = new ArrayList<>(list.size() + array.length);
        result.addAll(list);
        Collections.addAll(result, array);
        return result;
    }

    public static <K, V> Map<K, V> concat(Map<K, V>... maps) {
        return Arrays.stream(maps)
            .flatMap(e -> e.entrySet().stream())
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
    }

    public static <T> T[] concat(T[] a1, T... a2) {
        if (a1 == null || a1.length == 0) {
            return a2;
        }
        if (a2 == null || a2.length == 0) {
            return a1;
        }

        T[] result = newArray((Class<? extends T[]>) a1.getClass(), a1.length + a2.length);
        System.arraycopy(a1, 0, result, 0, a1.length);
        try {
            System.arraycopy(a2, 0, result, a1.length, a2.length);
        } catch (ArrayStoreException e) {
            Class<?> t1 = a1.getClass().getComponentType();
            Class<?> t2 = a2.getClass().getComponentType();
            throw t1.isAssignableFrom(t2) ? e : new IllegalArgumentException("Cannot store " + t2.getName() + " into " + t1.getName() + "[]", e);
        }
        return result;
    }

    public static <T> ArrayList<T> asArrayList(T... array) {
        ArrayList<T> list = new ArrayList<>();
        if (array != null && array.length > 0) {
            Collections.addAll(list, array);
        }
        return list;
    }

    public static <T> T[] newArray(Class<? extends T[]> arrayType, int length) {
        return arrayType.equals(Object[].class)
            ? (T[]) new Object[length]
            : (T[]) Array.newInstance(arrayType.getComponentType(), length);
    }

}
