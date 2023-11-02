/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.dispatch.route.count;

import cn.ponfee.disjob.common.spring.RedisKeyRenewal;
import cn.ponfee.disjob.core.base.JobConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.function.Function;

/**
 * Atomic counter based redis INCRBY command.
 *
 * @author Ponfee
 */
public class RedisAtomicCounter extends AtomicCounter {

    private final String counterRedisKey;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisKeyRenewal redisKeyRenewal;

    /**
     * Function<String, AtomicCounter>: group -> new RedisAtomicCounter(group, stringRedisTemplate)
     *
     * @param group               the job group
     * @param stringRedisTemplate the StringRedisTemplate
     * @see cn.ponfee.disjob.dispatch.route.RoundRobinExecutionRouter#RoundRobinExecutionRouter(Function)
     */
    public RedisAtomicCounter(String group,
                              StringRedisTemplate stringRedisTemplate) {
        this.counterRedisKey = JobConstants.DISJOB_KEY_PREFIX + ":route:counter:" + group;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisKeyRenewal = new RedisKeyRenewal(stringRedisTemplate, counterRedisKey);
    }

    @Override
    public long get() {
        String ret = stringRedisTemplate.opsForValue().get(counterRedisKey);
        if (StringUtils.isBlank(ret)) {
            return 0;
        }
        redisKeyRenewal.renewIfNecessary();
        return Long.parseLong(ret);
    }

    @Override
    public void set(long newValue) {
        stringRedisTemplate.opsForValue().set(counterRedisKey, Long.toString(newValue));
        redisKeyRenewal.renewIfNecessary();
    }

    @Override
    public long addAndGet(long delta) {
        Long value = stringRedisTemplate.opsForValue().increment(counterRedisKey, delta);
        redisKeyRenewal.renewIfNecessary();
        return value == null ? 0 : value;
    }

}
