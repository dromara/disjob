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

package cn.ponfee.disjob.supervisor.model;

import cn.ponfee.disjob.common.util.Numbers;
import cn.ponfee.disjob.common.util.UuidUtils;
import cn.ponfee.disjob.dispatch.route.count.AtomicCounter;
import cn.ponfee.disjob.dispatch.route.count.JdkAtomicCounter;
import cn.ponfee.disjob.supervisor.SpringBootTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author Ponfee
 */
public class RouteTest extends SpringBootTestBase<StringRedisTemplate> {

    @Test
    public void testAtomicCounter() {
        AtomicCounter counter = new JdkAtomicCounter(10);
        Assertions.assertEquals(10, counter.get());
        Assertions.assertEquals(10, counter.getAndIncrement());
        Assertions.assertEquals(16, counter.addAndGet(5));
        Assertions.assertEquals(16, counter.get());
        counter.set(100);
        Assertions.assertEquals(100, counter.get());
    }

    @Test
    public void testRedisAtomicCounter() {
        String key1 = "disjob:counter1:" + UuidUtils.uuid32();
        bean.opsForValue().set(key1, "10");
        Assertions.assertEquals(10, Numbers.toLong(bean.opsForValue().get(key1)));
        Assertions.assertEquals(11, bean.opsForValue().increment(key1));
        Assertions.assertEquals(12, bean.opsForValue().increment(key1));

        String key2 = "disjob:counter2:" + UuidUtils.uuid32();
        Assertions.assertNull(bean.opsForValue().get(key2));
        Assertions.assertEquals(1, bean.opsForValue().increment(key2));
        Assertions.assertEquals(2, bean.opsForValue().increment(key2));

        /*
        RedisAtomicCounter counter = new RedisAtomicCounter("disjob:counter3:" + UuidUtils.uuid32(), bean);
        long initValue = counter.get();
        Assertions.assertEquals(initValue, counter.getAndIncrement());
        Assertions.assertEquals(1, counter.get());
        Assertions.assertEquals(6 + initValue, counter.addAndGet(5));
        Assertions.assertEquals(6 + initValue, counter.get());
        counter.set(100);
        Assertions.assertEquals(100, counter.get());
        */
    }
}
