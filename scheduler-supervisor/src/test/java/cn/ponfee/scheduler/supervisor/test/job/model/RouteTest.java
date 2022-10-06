package cn.ponfee.scheduler.supervisor.test.job.model;

import cn.ponfee.scheduler.common.util.Numbers;
import cn.ponfee.scheduler.core.route.count.AtomicCounter;
import cn.ponfee.scheduler.core.route.count.JdkAtomicCounter;
import cn.ponfee.scheduler.core.route.count.RedisAtomicCounter;
import cn.ponfee.scheduler.supervisor.SpringBootTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

/**
 * @author Ponfee
 */
public class RouteTest extends SpringBootTestBase {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

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
        String key = "test_incr2131";
        stringRedisTemplate.opsForValue().set(key, "10");
        Assertions.assertEquals(10, Numbers.toLong(stringRedisTemplate.opsForValue().get(key)));
        Assertions.assertEquals(11, stringRedisTemplate.opsForValue().increment(key));
        Assertions.assertEquals(12, stringRedisTemplate.opsForValue().increment(key));

        RedisAtomicCounter counter = new RedisAtomicCounter("test", stringRedisTemplate);
        long init = counter.get();
        Assertions.assertEquals(1 + init, counter.getAndIncrement());
        Assertions.assertEquals(6 + init, counter.getAndAdd(5));
        Assertions.assertEquals(6 + init, counter.get());
        counter.set(100);
        Assertions.assertEquals(100, counter.get());
    }
}
