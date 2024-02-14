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

package cn.ponfee.disjob.supervisor.util;

import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;
import org.assertj.core.api.Condition;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AssertJ
 *
 * @author Ponfee
 */
public class AssertjTest {

    @Test
    public void test1() {
        List<String> stringList = Lists.newArrayList("A", "B", "C");
        assertThat(stringList).contains("A"); //true
        assertThat(stringList).doesNotContain("D"); //true
        assertThat(stringList).containsExactly("A", "B", "C"); //true
    }

    @Test
    public void test2() {
        List<String> stringList = Lists.newArrayList("A", "B", "C");
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(stringList).contains("A"); //true
        softly.assertThat(stringList).containsExactly("A", "B", "C"); //true
        // Don't forget to call SoftAssertions global verification!
        softly.assertAll();
    }

    @Test
    public void test3() {
        Integer integer1 = 127;
        Integer integer2 = 127;
        Integer integer3 = 128;
        Integer integer4 = 128;
        assertEquals(integer1, integer2, "127 is same");
        assertEquals(integer3, integer4, "128 is  equals");
        assertNotSame(integer3, integer4, "128 is not same");

        assertThat("").isEmpty();
        assertThat("555").isNotEmpty();
        assertThat("Gandalf the grey").containsAnyOf("grey", "black");

        Person person = new Person("tom");

        assertNotNull(person);
        assertEquals(person, person);
        assertSame(person, person);
        assertInstanceOf(person.getClass(), person);

        List<Person> list1 = Arrays.asList(person, new Person("test"));
        List<Person> list2 = Arrays.asList(new Person("abc"), new Person("test"));
        Condition<Person> condition1 = new Condition<>(list1::contains, "list1");
        Condition<Person> condition2 = new Condition<>(list2::contains, "list2");
        assertThat(person).is(anyOf(condition1, condition2));
        assertThat(person).isNot(allOf(condition1, condition2));
        assertThat(person).hasFieldOrProperty("name");
        assertThat(person).hasFieldOrPropertyWithValue("name", "tom");
    }

    @Getter
    @Setter
    public static class Person {
        String name;
        int age;

        public Person() { }

        public Person(String name) {
            this.name = name;
        }
    }

}
