/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.route.count;

import cn.ponfee.disjob.common.spring.RedisKeyRenewal;
import cn.ponfee.disjob.common.util.Numbers;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Atomic counter based redis INCRBY command.
 *
 * @author Ponfee
 */
public class RedisAtomicCounter extends AtomicCounter {

    private final String counterRedisKey;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisKeyRenewal redisKeyRenewal;

    public RedisAtomicCounter(String redisCounterKey,
                              StringRedisTemplate stringRedisTemplate) {
        this.counterRedisKey = "route:counter:" + redisCounterKey;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisKeyRenewal = new RedisKeyRenewal(stringRedisTemplate, counterRedisKey);

        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(counterRedisKey))) {
            // initialize
            set(1);
        }
    }

    @Override
    public long get() {
        String ret = stringRedisTemplate.opsForValue().get(counterRedisKey);
        redisKeyRenewal.renewIfNecessary();
        return Numbers.toLong(ret);
    }

    @Override
    public void set(long newValue) {
        stringRedisTemplate.opsForValue().set(counterRedisKey, Long.toString(newValue));
        redisKeyRenewal.renewIfNecessary();
    }

    @Override
    public long getAndAdd(long delta) {
        Long value = stringRedisTemplate.opsForValue().increment(counterRedisKey, delta);
        redisKeyRenewal.renewIfNecessary();
        return value == null ? 0 : value;
    }

}
