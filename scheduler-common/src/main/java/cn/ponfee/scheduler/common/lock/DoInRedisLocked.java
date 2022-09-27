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
    public <T> T apply(Callable<T> caller) {
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
