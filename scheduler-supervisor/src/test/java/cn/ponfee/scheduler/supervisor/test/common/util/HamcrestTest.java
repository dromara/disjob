/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.test.common.util;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.beans.HasProperty;
import org.hamcrest.beans.HasPropertyWithValue;
import org.hamcrest.core.*;
import org.hamcrest.text.IsEmptyString;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Hamcrest
 *
 * @author Ponfee
 */
public class HamcrestTest {

    @Test
    public void test1() {
        Integer integer1 = 127;
        Integer integer2 = 127;
        Integer integer3 = 128;
        Integer integer4 = 128;
        MatcherAssert.assertThat("127 is same", integer1 == integer2);
        MatcherAssert.assertThat("128 is not same", integer3 != integer4);
        IsSame<Integer> integerIsSame = new IsSame<>(integer3);
        MatcherAssert.assertThat("128 is not same", !integerIsSame.matches(integer4));

        Person person = new Person("tom");

        //
        IsAnything<Person> anything = new IsAnything<>();
        boolean matched = anything.matches(person);
        System.out.println("anything:" + matched);

        Matcher<String> isEmptyString = IsEmptyString.emptyString();
        matched = isEmptyString.matches("555");
        System.out.println("isEmptyString:" + matched);

        IsEqual<Person> isEqual = new IsEqual<>(person);
        matched = isEqual.matches(person);
        System.out.println("isEqual:" + matched);

        IsNull<Person> isNull = new IsNull<>();
        matched = isNull.matches(person);
        System.out.println("isNull:" + matched);

        IsSame<Person> isSame = new IsSame<>(person);
        matched = isSame.matches(person);
        System.out.println("isEqual:" + matched);

        Is<Person> is = new Is<>(isEqual);
        matched = is.matches(person);
        System.out.println("is:" + matched);

        IsNot<Person> isNot = new IsNot<>(isEqual);
        matched = isNot.matches(person);
        System.out.println("isNot:" + matched);


        IsInstanceOf isInstanceOf = new IsInstanceOf(person.getClass());
        matched = isInstanceOf.matches(person);
        System.out.println("isInstanceOf:" + matched);


        List<Matcher<? super Person>> matchers = new ArrayList<>();
        matchers.add(is);
        matchers.add(isSame);
        AllOf<Person> allOf = new AllOf<>(matchers);
        matched = allOf.matches(person);
        System.out.println("allOf:" + matched);


        AnyOf<Person> anyOf = new AnyOf<>(matchers);
        matched = anyOf.matches(person);
        System.out.println("anyOf:" + matched);


        HasProperty<Person> hasProperty = new HasProperty<>("name");
        matched = hasProperty.matches(person);
        System.out.println("hasProperty:" + matched);

        HasPropertyWithValue<Person> hasPropertyWithValue = new HasPropertyWithValue<>("name", new IsEqual<>("tom"));

        matched = hasPropertyWithValue.matches(person);
        System.out.println("hasPropertyWithValue:" + matched);
        //org.hamcrest.Matchers.allOf()
        //org.hamcrest.CoreMatchers.allOf()
    }

    public static class Person {
        String name;
        int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public Person(String name) {
            this.name = name;
        }
    }

}
