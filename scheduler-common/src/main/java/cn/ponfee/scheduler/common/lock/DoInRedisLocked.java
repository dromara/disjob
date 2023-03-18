/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * Do in redis locked.
 *
 * @author Ponfee
 */
public class DoInRedisLocked implements DoInLocked {

    private static final Logger LOG = LoggerFactory.getLogger(DoInRedisLocked.class);

    private final RedisLock redisLock;

    public DoInRedisLocked(RedisLock redisLock) {
        this.redisLock = redisLock;
    }

    @Override
    public <T> T action(Callable<T> caller) {
        if (!redisLock.tryLock()) {
            return null;
        }

        try {
            return caller.call();
        } catch (Exception e) {
            LOG.error("Do in redis lock occur error.", e);
            return null;
        } finally {
            redisLock.unlock();
        }
    }
}
