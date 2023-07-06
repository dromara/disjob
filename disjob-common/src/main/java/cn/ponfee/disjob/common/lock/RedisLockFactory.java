/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.lock;

import org.springframework.data.redis.core.RedisTemplate;

/**
 * Redis lock factory
 *
 * @author Ponfee
 */
public class RedisLockFactory {

    /**
     * Spring redis template.
     */
    private final transient RedisTemplate<?, ?> redisTemplate;

    /**
     * Wait lock period sleep milliseconds.
     */
    private final long sleepMillis;

    public RedisLockFactory(RedisTemplate<?, ?> redisTemplate) {
        this(redisTemplate, 50);
    }

    public RedisLockFactory(RedisTemplate<?, ?> redisTemplate, long sleepMillis) {
        this.redisTemplate = redisTemplate;
        this.sleepMillis = Math.max(10, sleepMillis);
    }

    public RedisLock getLock(String lockKey, int timeoutMillis) {
        return new RedisLock(redisTemplate, lockKey, timeoutMillis, sleepMillis);
    }

}
