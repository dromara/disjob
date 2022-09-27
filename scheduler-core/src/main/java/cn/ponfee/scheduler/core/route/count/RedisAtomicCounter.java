package cn.ponfee.scheduler.core.route.count;

import cn.ponfee.scheduler.common.util.Numbers;
import cn.ponfee.scheduler.core.redis.RedisKeyUtils;
import org.springframework.data.redis.core.RedisTemplate;

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
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Next refresh interval time milliseconds.
     */
    private volatile long nextRenewTimeMillis = 0L;

    public RedisAtomicCounter(String redisCounterKey,
                              RedisTemplate<String, String> redisTemplate) {
        this.counterRedisKey = "route:counter:" + redisCounterKey;
        this.redisTemplate = redisTemplate;

        if (!this.redisTemplate.hasKey(this.counterRedisKey)) {
            // initialize
            set(1);
        }
    }

    @Override
    public long get() {
        String ret = redisTemplate.opsForValue().get(counterRedisKey);
        renewIfNecessary();
        return Numbers.toLong(ret);
    }

    @Override
    public void set(long newValue) {
        redisTemplate.opsForValue().set(counterRedisKey, Long.toString(newValue));
        renewIfNecessary();
    }

    @Override
    public long getAndAdd(long delta) {
        Long value = redisTemplate.opsForValue().increment(counterRedisKey, delta);
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
            redisTemplate.expire(counterRedisKey, RedisKeyUtils.REDIS_KEY_TTL_SECONDS, TimeUnit.MILLISECONDS);
            nextRenewTimeMillis = currentTimeMillis + RedisKeyUtils.REDIS_KEY_TTL_RENEW_INTERVAL_MILLIS;
        } finally {
            lock.unlock();
        }
    }

}
