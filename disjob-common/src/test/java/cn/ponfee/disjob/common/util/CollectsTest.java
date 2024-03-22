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
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    private static final String test = "xxx";
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
            .hasMessage("Can not set static final java.lang.String field cn.ponfee.disjob.common.util.CollectsTest.STR to java.lang.String");

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
