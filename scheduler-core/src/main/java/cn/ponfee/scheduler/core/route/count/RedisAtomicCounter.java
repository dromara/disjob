package cn.ponfee.scheduler.core.route.count;

import cn.ponfee.scheduler.common.util.Numbers;
import cn.ponfee.scheduler.core.base.JobConstants;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Atomic counter based redis INCRBY command.
 *
 * @author Ponfee
 */
public class RedisAtomicCounter extends AtomicCounter {

    private final Lock lock = new ReentrantLock();
    private final String counterRedisKey;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Next refresh interval time milliseconds.
     */
    private volatile long nextRenewTimeMillis = 0L;

    public RedisAtomicCounter(String redisCounterKey,
                              StringRedisTemplate stringRedisTemplate) {
        this.counterRedisKey = "route:counter:" + redisCounterKey;
        this.stringRedisTemplate = stringRedisTemplate;

        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(counterRedisKey))) {
            // initialize
            set(1);
        }
    }

    @Override
    public long get() {
        String ret = stringRedisTemplate.opsForValue().get(counterRedisKey);
        renewIfNecessary();
        return Numbers.toLong(ret);
    }

    @Override
    public void set(long newValue) {
        stringRedisTemplate.opsForValue().set(counterRedisKey, Long.toString(newValue));
        renewIfNecessary();
    }

    @Override
    public long getAndAdd(long delta) {
        Long value = stringRedisTemplate.opsForValue().increment(counterRedisKey, delta);
        renewIfNecessary();
        return value == null ? 0 : value;
    }

    private void renewIfNecessary() {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis < nextRenewTimeMillis) {
            return;
        }

        if (!lock.tryLock()) {
            return;
        }
        try {
            if (currentTimeMillis < nextRenewTimeMillis) {
                return;
            }
            stringRedisTemplate.expire(counterRedisKey, JobConstants.REDIS_KEY_TTL_SECONDS, TimeUnit.MILLISECONDS);
            nextRenewTimeMillis = currentTimeMillis + JobConstants.REDIS_KEY_TTL_RENEW_INTERVAL_MILLIS;
        } finally {
            lock.unlock();
        }
    }

}
