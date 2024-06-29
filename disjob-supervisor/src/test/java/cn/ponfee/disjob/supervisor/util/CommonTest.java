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
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.Operation;
import cn.ponfee.disjob.core.enums.RedeployStrategy;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

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
            RedeployStrategy.RESTART,
            1,
            "JobHandler测试中文乱码。",
            new Worker("default", "workerId", "host", 1)
        );
        System.out.println(param1);
        ExecuteTaskParam param2 = ExecuteTaskParam.deserialize(param1.serialize());
        Assertions.assertNotSame(param1, param2);
        Assertions.assertEquals(param1.toString(), param2.toString());
        Assertions.assertEquals(param1.getSupervisorToken(), param2.getSupervisorToken());
        Assertions.assertEquals(param1.getWorker(), param2.getWorker());
        Assertions.assertEquals(param1.getJobHandler(), param2.getJobHandler());
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
        for (int i = 0; i < 10000; i++) {
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
                                                          RedeployStrategy redeployStrategy,
                                                          int executeTimeout,
                                                          String jobHandler,
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
        param.setRedeployStrategy(redeployStrategy);
        param.setExecuteTimeout(executeTimeout);
        param.setSupervisorToken("supervisor token");
        param.setWorker(worker);
        param.setJobHandler(jobHandler);
        return param;
    }

}
