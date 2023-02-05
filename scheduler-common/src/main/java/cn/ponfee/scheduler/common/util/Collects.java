/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.util;

import com.google.common.collect.ImmutableList;
import org.springframework.util.Assert;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;

/**
 * Collection utilities
 *
 * @author Ponfee
 */
public class Collects {

    /**
     * Gets the first element for list
     *
     * @param list the list
     * @param <T>  the list element type
     * @return first element of list
     */
    public static <T> T getFirst(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return (list instanceof Deque) ? ((Deque<T>) list).getFirst() : list.get(0);
    }

    /**
     * Gets the last element for list
     *
     * @param list the list
     * @param <T>  the list element type
     * @return last element of list
     */
    public static <T> T getLast(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        //return list.stream().reduce((a, b) -> b).orElse(null);
        return (list instanceof Deque) ? ((Deque<T>) list).getLast() : list.get(list.size() - 1);
    }

    public static <T> T get(T[] array, int index) {
        if (array == null) {
            return null;
        }
        return index < array.length ? array[index] : null;
    }

    public static <T> T get(List<T> list, int index) {
        if (list == null) {
            return null;
        }
        return index < list.size() ? list.get(index) : null;
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

    public static <S, T> List<T> convert(List<S> source, Function<S, T> mapper) {
        if (source == null) {
            return null;
        }
        if (source.isEmpty()) {
            return Collections.emptyList();
        }
        ImmutableList.Builder<T> builder = ImmutableList.builderWithExpectedSize(source.size());
        source.stream().map(mapper).forEach(builder::add);
        return builder.build();
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

    public static <T> T[] newArray(Class<? extends T[]> newType, int length) {
        return newType.equals(Object[].class)
            ? (T[]) new Object[length]
            : (T[]) Array.newInstance(newType.getComponentType(), length);
    }

}
