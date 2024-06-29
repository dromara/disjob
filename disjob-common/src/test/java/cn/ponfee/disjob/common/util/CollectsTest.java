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

package cn.ponfee.disjob.common.util;

import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.tuple.Tuple2;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Collects test
 *
 * @author Ponfee
 */
public class CollectsTest {

    @Test
    public void testTruncate() {
        assertThat(Collects.truncate(null, 2)).isNull();
        assertThat(Collects.truncate(Collections.emptySet(), 2)).isEmpty();
        assertThat(Collects.truncate(Sets.newSet(1), 0)).size().isEqualTo(1);
        assertThat(Collects.truncate(Sets.newSet(1, 2, 3, 4, 5), 3)).size().isEqualTo(3);
        assertThat(Stream.of().anyMatch(Objects::isNull)).isFalse();
        assertThat(Stream.of(1).anyMatch(Objects::isNull)).isFalse();
        assertThat(Stream.of(1, null).anyMatch(Objects::isNull)).isTrue();
    }

    @Test
    public void testNewArray() {
        List<List<Integer>> list = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(2, 3, 4), Arrays.asList(6, 7, 8));
        Integer[] array1 = list.stream().flatMap(List::stream).toArray(length -> Collects.newArray(Integer[].class, length));
        System.out.println(Arrays.toString(array1));
        assertThat(array1).hasSize(9);

        Object[] array2 = list.stream().flatMap(List::stream).toArray(length -> Collects.newArray(Object[].class, length));
        System.out.println(Arrays.toString(array2));
        assertThat(array2).hasSize(9);
    }

    @Test
    public void testSort() {
        List<Tuple2<String, Long>> list = new ArrayList<>();
        list.add(Tuple2.of("A", 3L));
        list.add(Tuple2.of("B", 2L));
        list.add(Tuple2.of("C", 1L));
        list.add(Tuple2.of("D", 4L));
        Comparator<Tuple2<String, Long>> comparator = Comparator.comparingLong(e -> e.b);

        list.sort(comparator);
        System.out.println(list);
        Tuple2<String, Long> first = list.get(0);
        assertThat(first.a).isEqualTo("C");
        first.b += 1L;

        list.sort(comparator);
        System.out.println(list);
        first = list.get(0);
        assertThat(first.a).isEqualTo("C");
        first.b += 1L;

        list.sort(comparator);
        System.out.println(list);
        first = list.get(0);
        assertThat(first.a).isEqualTo("B");
        first.b += 1L;

        list.sort(comparator);
        System.out.println(list);
        first = list.get(0);
        assertThat(first.a).isEqualTo("B");
        first.b += 1L;

        list.sort(comparator);
        System.out.println(list);
        first = list.get(0);
        assertThat(first.a).isEqualTo("C");
        first.b += 1L;

        list.sort(comparator);
        System.out.println(list);
        first = list.get(0);
        assertThat(first.a).isEqualTo("A");
        first.b += 1L;
    }

    @Test
    public void testConcat() {
        String[] b = {"a", "b", "c"};

        Integer[] a1 = {1, 2, 3};
        assertThatThrownBy(() -> Collects.concat(a1, b))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot store java.lang.String into java.lang.Integer[]");

        Object[] a2 = a1;
        assertThatThrownBy(() -> Collects.concat(a2, b))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot store java.lang.String into java.lang.Integer[]");

        Object[] a3 = {1, 2, 3};
        assertThat(Collects.concat(a3, b)).hasSize(6);

        assertThatThrownBy(() -> Collects.concat(b, a3))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot store java.lang.Object into java.lang.String[]");
    }

    private static String test = "xxx";
    private static final String STR = "123";

    @Test
    public void testReflect() throws IllegalAccessException {
        // static field
        Field f = FieldUtils.getField(CollectsTest.class, "test", true);
        assertThat("xxx").isEqualTo(test);
        assertThat("xxx").isEqualTo(FieldUtils.readField(f, (Object) null));
        FieldUtils.writeField(f, (Object) null, "yyy", true);
        assertThat("yyy").isEqualTo(test);
        assertThat("yyy").isEqualTo(FieldUtils.readField(f, (Object) null));
        test = "123";
        assertThat("123").isEqualTo(test);

        // static final field
        Field f1 = FieldUtils.getField(CollectsTest.class, "STR", true);
        Field f2 = FieldUtils.getField(CollectsTest.class, "STR", true);
        assertThat(f1).isSameAs(f1);
        assertThat(f1 == f2).isFalse();
        assertThat(f1).isNotSameAs(f2); // f1 != f2
        assertThat(f1).isEqualTo(f2);

        assertThat("123").isEqualTo(STR);
        assertThat("123").isEqualTo(FieldUtils.readField(f1, (Object) null));

        assertThatThrownBy(() -> FieldUtils.writeField(f1, (Object) null, "abc", true))
            .isInstanceOf(IllegalAccessException.class)
            // Windows message: Can not set static final java.lang.String field cn.ponfee.disjob.common.util.CollectsTest.STR to null value
            // Linux message  : Can not set static final java.lang.String field cn.ponfee.disjob.common.util.CollectsTest.STR to java.lang.String
            .hasMessageStartingWith("Can not set static final java.lang.String field cn.ponfee.disjob.common.util.CollectsTest.STR to ");

        Fields.put(CollectsTest.class, f1, "abc");
        assertThat("123").isEqualTo(STR); // 编译时直接替换为`123`
        assertThat("abc").isEqualTo(FieldUtils.readField(f1, (Object) null));

        Method m1 = MethodUtils.getMatchingMethod(ClassUtils.class, "decodeURL", URL.class);
        Method m2 = MethodUtils.getMatchingMethod(ClassUtils.class, "decodeURL", URL.class);
        assertThat(m1).isSameAs(m1);
        assertThat(m1 == m2).isFalse();
        assertThat(m1).isNotSameAs(m2);
        assertThat(m1).isEqualTo(m2);
    }

}
