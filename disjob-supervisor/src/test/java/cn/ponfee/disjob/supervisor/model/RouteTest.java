/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.model;

import cn.ponfee.disjob.common.util.Numbers;
import cn.ponfee.disjob.core.route.count.AtomicCounter;
import cn.ponfee.disjob.core.route.count.JdkAtomicCounter;
import cn.ponfee.disjob.core.route.count.RedisAtomicCounter;
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
        Assertions.assertEquals(11, counter.getAndAdd(5));
        Assertions.assertEquals(16, counter.get());
        counter.set(100);
        Assertions.assertEquals(100, counter.get());
    }

    @Test
    public void testRedisAtomicCounter() {
        String key = "key_counter";
        bean.opsForValue().set(key, "10");
        Assertions.assertEquals(10, Numbers.toLong(bean.opsForValue().get(key)));
        Assertions.assertEquals(11, bean.opsForValue().increment(key));
        Assertions.assertEquals(12, bean.opsForValue().increment(key));

        RedisAtomicCounter counter = new RedisAtomicCounter("test", bean);
        long init = counter.get();
        Assertions.assertEquals(1 + init, counter.getAndIncrement());
        Assertions.assertEquals(6 + init, counter.getAndAdd(5));
        Assertions.assertEquals(6 + init, counter.get());
        counter.set(100);
        Assertions.assertEquals(100, counter.get());
    }
}
