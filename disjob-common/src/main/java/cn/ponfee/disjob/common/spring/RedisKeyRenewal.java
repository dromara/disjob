/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Renew redis key ttl based spring redis template.
 *
 * @author Ponfee
 */
public class RedisKeyRenewal {

    private static final Logger LOG = LoggerFactory.getLogger(RedisKeyRenewal.class);

    private final Lock lock = new ReentrantLock();

    private final RedisTemplate<?, ?> redisTemplate;
    private final byte[] byteKey;
    private final String stringKey;
    private final long ttlMillis;
    private final long intervalMillis;

    private volatile long nextRenewTimeMillis = 0;

    public RedisKeyRenewal(RedisTemplate<?, ?> redisTemplate, String stringKey) {
        this(redisTemplate, stringKey.getBytes(StandardCharsets.UTF_8));
    }

    public RedisKeyRenewal(RedisTemplate<?, ?> redisTemplate, byte[] byteKey) {
        this(redisTemplate, byteKey, 30L * 86400 * 1000, 600L * 1000);
    }

    public RedisKeyRenewal(RedisTemplate<?, ?> redisTemplate, String stringKey, long ttlMillis, long intervalMillis) {
        this(redisTemplate, stringKey.getBytes(StandardCharsets.UTF_8), ttlMillis, intervalMillis);
    }

    public RedisKeyRenewal(RedisTemplate<?, ?> redisTemplate, byte[] byteKey, long ttlMillis, long intervalMillis) {
        this.redisTemplate = redisTemplate;
        this.byteKey = byteKey;
        this.stringKey = new String(byteKey, StandardCharsets.UTF_8);
        this.ttlMillis = ttlMillis;
        this.intervalMillis = Math.min(intervalMillis, ttlMillis - 1);
    }

    /**
     * Renew redis key ttl if necessary.
     */
    public void renewIfNecessary() {
        if (System.currentTimeMillis() < nextRenewTimeMillis) {
            return;
        }

        if (lock.tryLock()) {
            try {
                if (System.currentTimeMillis() < nextRenewTimeMillis) {
                    return;
                }

                redisTemplate.execute((RedisCallback<?>) conn -> conn.pExpire(byteKey, ttlMillis));
                this.nextRenewTimeMillis = System.currentTimeMillis() + intervalMillis;
                LOG.debug("Renewed redis key '{}' successful.", stringKey);
            } catch (Exception e) {
                LOG.warn("Renew redis key '" + stringKey + "' occur error.", e);
            } finally {
                lock.unlock();
            }
        }
    }

}
