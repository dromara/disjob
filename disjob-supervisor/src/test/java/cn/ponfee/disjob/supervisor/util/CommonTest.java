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

import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.tuple.Tuple2;
import cn.ponfee.disjob.common.util.ClassUtils;
import cn.ponfee.disjob.common.util.Files;
import cn.ponfee.disjob.common.util.Numbers;
import cn.ponfee.disjob.common.util.TextBoxPrinter;
import cn.ponfee.disjob.core.base.CoreUtils;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.Operation;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.enums.ShutdownStrategy;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;
import org.assertj.core.api.Condition;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ponfee
 */
public class CommonTest {

    @Test
    public void testPath() {
        List<String> list = Collections.emptyList();
        Assertions.assertEquals("classpath*:xml/*.xml", path(list, 0));

        list = Arrays.asList("a");
        Assertions.assertEquals("classpath*:a/xml/*.xml", path(list, -1));
        Assertions.assertEquals("classpath*:a/**/xml/*.xml", path(list, 0));
        Assertions.assertEquals("classpath*:/**/a/xml/*.xml", path(list, 1));

        list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("classpath*:a/b/c/xml/*.xml", path(list, -1));
        Assertions.assertEquals("classpath*:a/b/c/**/xml/*.xml", path(list, 0));
        Assertions.assertEquals("classpath*:a/b/**/c/xml/*.xml", path(list, 1));
        Assertions.assertEquals("classpath*:a/**/b/c/xml/*.xml", path(list, 2));
        Assertions.assertEquals("classpath*:/**/a/b/c/xml/*.xml", path(list, 3));
        Assertions.assertEquals("classpath*:/**/a/b/c/xml/*.xml", path(list, 4));
    }

    private static String path(List<String> list, int wildcardLastIndex) {
        String path;
        if (list.isEmpty()) {
            path = "";
        } else if (wildcardLastIndex == 0) {
            path = String.join("/", list) + "/**/";
        } else if (wildcardLastIndex < 0) {
            path = String.join("/", list) + "/";
        } else if (list.size() <= wildcardLastIndex) {
            path = "/**/" + String.join("/", list) + "/";
        } else {
            int pos = list.size() - wildcardLastIndex;
            path = String.join("/", list.subList(0, pos)) + "/**/" + String.join("/", list.subList(pos, list.size())) + "/";
        }
        return MessageFormat.format("classpath*:{0}xml/*.xml", path);
    }

    @Test
    public void testURLString() throws Exception {
        URL url1 = new URL("https://www.oschina.net/search?scope=bbs&q=C语言");
        Assertions.assertEquals("https://www.oschina.net/search?scope=bbs&q=C语言", url1.toString());

        Assertions.assertEquals("/search?scope=bbs&q=C语言", url1.getFile());
        Assertions.assertEquals("https", url1.getProtocol());
        Assertions.assertEquals("www.oschina.net", url1.getHost());
        Assertions.assertEquals(-1, url1.getPort());
        Assertions.assertEquals("/search", url1.getPath());
        Assertions.assertEquals("scope=bbs&q=C语言", url1.getQuery());
        Assertions.assertNull(url1.getRef());
        Assertions.assertNull(url1.getUserInfo());
        Assertions.assertEquals("www.oschina.net", url1.getAuthority());
        Assertions.assertEquals(443, url1.getDefaultPort());

        URL url2 = new URL("https://www.oschina.net/search?scope=bbs&q=C%E8%AF%AD%E8%A8%80");
        Assertions.assertEquals("https://www.oschina.net/search?scope=bbs&q=C%E8%AF%AD%E8%A8%80", url2.toString());
        Assertions.assertEquals("https://www.oschina.net/search?scope=bbs&q=C语言", URLDecoder.decode(url2.toString(), Files.UTF_8));

        System.out.println(ClassUtils.getClassFilePath(ClassUtils.class));
        System.out.println(ClassUtils.getClassFilePath(org.apache.commons.lang3.StringUtils.class));
        System.out.println(ClassUtils.getClasspath(ClassUtils.class));
        System.out.println(ClassUtils.getClasspath(org.apache.commons.lang3.StringUtils.class));
        System.out.println(ClassUtils.getClasspath());
    }

    @Test
    public void testTaskParam() {
        ExecuteTaskParam param1 = createExecuteTaskParam(
            Operation.TRIGGER,
            ThreadLocalRandom.current().nextLong(),
            ThreadLocalRandom.current().nextLong(),
            1L,
            ThreadLocalRandom.current().nextLong(),
            ThreadLocalRandom.current().nextLong(),
            JobType.GENERAL,
            RouteStrategy.ROUND_ROBIN,
            ShutdownStrategy.RESUME,
            1,
            "JobExecutor测试中文乱码。",
            new Worker("default", "workerId", "host", 1)
        );
        System.out.println(param1);
        ExecuteTaskParam param2 = ExecuteTaskParam.deserialize(param1.serialize());
        Assertions.assertNotSame(param1, param2);
        Assertions.assertEquals(param1.toString(), param2.toString());
        Assertions.assertEquals(param1.getSupervisorAuthenticationToken(), param2.getSupervisorAuthenticationToken());
        Assertions.assertEquals(param1.getWorker(), param2.getWorker());
        Assertions.assertEquals(param1.getJobExecutor(), param2.getJobExecutor());
    }

    @Test
    public void testTime() {
        System.out.println((System.currentTimeMillis() / 1000) * 1000 + 999);
    }

    @Test
    public void testArrayPartition() {
        int[] ints = IntStream.range(0, 5).toArray();
        System.out.println(Arrays.toString(ints));
        Collects.partition(ints, 2)
            .stream()
            .peek(e -> System.out.println(Arrays.toString(e)))
            .skip(1)
            .forEach(e -> Assertions.assertTrue(Arrays.equals(new int[]{3, 4}, e)));

        System.out.println("-----\n");
        ints = IntStream.range(0, 6).toArray();
        System.out.println(Arrays.toString(ints));
        Collects.partition(ints, 2)
            .stream()
            .peek(e -> System.out.println(Arrays.toString(e)))
            .skip(1)
            .forEach(e -> Assertions.assertTrue(Arrays.equals(new int[]{3, 4, 5}, e)));

        System.out.println("-----\n");
        ints = IntStream.range(0, 3).toArray();
        System.out.println(Arrays.toString(ints));
        Collects.partition(ints, 2)
            .stream()
            .peek(e -> System.out.println(Arrays.toString(e)))
            .skip(1)
            .forEach(e -> Assertions.assertTrue(Arrays.equals(new int[]{2}, e)));


        System.out.println("-----\n");
        ints = IntStream.range(0, 1).toArray();
        System.out.println(Arrays.toString(ints));
        Collects.partition(ints, 5)
            .stream()
            .peek(e -> System.out.println(Arrays.toString(e)))
            .skip(1)
            .forEach(e -> Assertions.assertTrue(Arrays.equals(new int[]{0}, e)));

        System.out.println("-----\n");
        ints = IntStream.range(0, 256).toArray();
        System.out.println("origin: " + Arrays.toString(ints));
        Collects.partition(ints, 5)
            .stream()
            .peek(e -> System.out.println("partitioned: " + Arrays.toString(e)))
            .skip(4)
            .peek(e -> System.out.println("last: " + Arrays.toString(e)))
            .forEach(e -> Assertions.assertTrue(Arrays.equals(new int[]{205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255}, e)));
    }

    @Test
    public void testNumberSplit() {
        System.out.println(Arrays.toString(Numbers.slice(3, 4)));
        System.out.println("------\n");
        System.out.println(Numbers.partition(31, 4));
        System.out.println(Numbers.partition(0, 1));
        System.out.println(Numbers.partition(0, 4));
        System.out.println(Numbers.partition(3, 4));
        System.out.println(Numbers.partition(6, 4));
        System.out.println(Numbers.partition(5, 2));
        System.out.println(Numbers.partition(47, 1));
        System.out.println(Numbers.partition(47, 2));
        System.out.println(Numbers.partition(256, 2));
        System.out.println(Numbers.partition(256, 4));
        for (int i = 0; i < 100; i++) {
            int number = ThreadLocalRandom.current().nextInt(100000) + 1, size = ThreadLocalRandom.current().nextInt(31) + 1;
            List<Tuple2<Integer, Integer>> split = Numbers.partition(number, size);
            Assertions.assertTrue(Collects.getFirst(split).a == 0);
            Assertions.assertTrue(Collects.getFirst(split).b == (number + size - 1) / size - 1);
            Assertions.assertTrue(Collects.getLast(split).b == number - 1);
            Assertions.assertTrue(Collects.getLast(Numbers.partition(number, size)).b == number - 1);
        }
    }


    public static ExecuteTaskParam createExecuteTaskParam(Operation operation,
                                                          long taskId,
                                                          long instanceId,
                                                          Long wnstanceId,
                                                          long triggerTime,
                                                          long jobId,
                                                          JobType jobType,
                                                          RouteStrategy routeStrategy,
                                                          ShutdownStrategy shutdownStrategy,
                                                          int executeTimeout,
                                                          String jobExecutor,
                                                          Worker worker) {
        ExecuteTaskParam param = new ExecuteTaskParam();
        param.setOperation(operation);
        param.setTaskId(taskId);
        param.setInstanceId(instanceId);
        param.setWnstanceId(wnstanceId);
        param.setTriggerTime(triggerTime);
        param.setJobId(jobId);
        param.setJobType(jobType);
        param.setRouteStrategy(routeStrategy);
        param.setShutdownStrategy(shutdownStrategy);
        param.setExecuteTimeout(executeTimeout);
        param.setSupervisorAuthenticationToken("supervisor token");
        param.setWorker(worker);
        param.setJobExecutor(jobExecutor);
        return param;
    }

    @Test
    public void testCache1() throws InterruptedException {
        Cache<Long, String> cache = CacheBuilder.newBuilder()
            .initialCapacity(10)
            .maximumSize(100)
            .expireAfterAccess(Duration.ofMillis(200))
            .build();
        cache.put(1L, "a");
        cache.put(2L, "b");
        cache.put(3L, "c");

        Thread.sleep(100);
        cache.cleanUp();
        Assertions.assertEquals(3, cache.size());
        Thread.sleep(130);
        Assertions.assertEquals(3, cache.size());
        cache.cleanUp();
        Assertions.assertEquals(0, cache.size());
    }

    @Test
    public void testCache2() throws InterruptedException, ExecutionException {
        LoadingCache<Long, String> cache = CacheBuilder.newBuilder()
            .initialCapacity(10)
            .maximumSize(100)
            .expireAfterAccess(Duration.ofMillis(200))
            .recordStats()
            .build(new CacheLoader<Long, String>() {
                @Override
                public String load(Long key) {
                    try {
                        Thread.sleep(ThreadLocalRandom.current().nextInt(1000));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return key.toString();
                }
            });
        cache.put(111L, "a");
        cache.put(222L, "b");
        cache.put(333L, "c");

        Thread.sleep(100);
        cache.cleanUp();
        Assertions.assertEquals(3, cache.size());
        System.out.println(CoreUtils.buildCacheStats(cache, "test"));
        Assertions.assertNull(cache.getIfPresent(0L));
        System.out.println(CoreUtils.buildCacheStats(cache, "test cache2cache1"));
        Assertions.assertEquals("a", cache.getIfPresent(111L));
        System.out.println(CoreUtils.buildCacheStats(cache, "test cache3cache3"));
        Thread.sleep(130);
        Assertions.assertEquals(3, cache.size());
        cache.cleanUp();
        Assertions.assertEquals(1, cache.size());

        Assertions.assertEquals("123", cache.get(123L));
        System.out.println(CoreUtils.buildCacheStats(cache, "test4"));

        System.out.println();
        System.out.println(TextBoxPrinter.print("abc"));
        System.out.println(TextBoxPrinter.print("abc", "def"));
        System.out.println(TextBoxPrinter.print("abc", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));

    }

    @Test
    public void testToMapHasNull() {
        Assertions.assertThrows(
            NullPointerException.class,
            () -> Arrays.stream(new Tuple2[]{Tuple2.of("a", "123"), Tuple2.of("b", null)}).collect(Collectors.toMap(t -> t.a, t -> t.b, (v1, v2) -> v2))
        );
    }

    @Test
    public void testToMapNoneNull() {
        Arrays.stream(new Tuple2[]{Tuple2.of("a", "123"), Tuple2.of("b", "abc")})
            .collect(Collectors.toMap(t -> t.a, t -> t.b, (v1, v2) -> v2));
    }

    @Test
    public void testIntStream() {
        String mainThread = Thread.currentThread().getName();
        System.out.println(mainThread);
        System.out.println();
        System.out.println("=================================");
        System.out.println("Using Sequential Stream");
        System.out.println("=================================");
        int[] array = {1};
        int length = array.length;
        IntStream intArrStream = Arrays.stream(array);
        intArrStream.forEach(s -> System.out.println(s + " " + Thread.currentThread().getName()));

        System.out.println("\n");

        System.out.println("=================================");
        System.out.println("Using Parallel Stream");
        System.out.println("=================================");
        IntStream intParallelStream = Arrays.stream(array).parallel();
        intParallelStream.forEach(s ->
            {
                System.out.println(s + " " + Thread.currentThread().getName());
                if (length == 1) {
                    Assertions.assertEquals(Thread.currentThread().getName(), mainThread);
                }
            }
        );
    }

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
