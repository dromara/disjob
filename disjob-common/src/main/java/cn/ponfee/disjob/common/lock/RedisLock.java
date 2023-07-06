/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.lock;

import cn.ponfee.disjob.common.util.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.util.Assert;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Distributes lock based redis(unlock使用redis lua script)
 * <p>可重入锁的一般场景：当前线程多次调用含有锁操作的函数、当前线程含有锁操作的函数自身调用
 *
 * <pre>
 * class X {
 *   public void m() {
 *     Lock lock = new RedisLock(redisTemplate, "lockKey", 5000, 30);
 *     lock.lock();  // block until acquire lock or timeout
 *     try {
 *       // ... method body
 *     } finally {
 *       lock.unlock()
 *     }
 *   }
 * }
 *
 * class Y {
 *   public void m() {
 *     Lock lock = new RedisLock(redisTemplate, "lockKey", 5000, 30);
 *     if (!lock.tryLock()) return;
 *     try {
 *       // ... method body
 *     } finally {
 *       lock.unlock();
 *     }
 *   }
 * }
 *
 * class Z {
 *   public void m() {
 *     Lock lock = new RedisLock(redisTemplate, "lockKey", 5000, 30);
 *     // auto timeout release lock
 *     if (!lock.tryLock(100, TimeUnit.MILLISECONDS)) return;
 *     try {
 *       // ... method body
 *     } finally {
 *       lock.unlock();
 *     }
 *   }
 * }
 * </pre>
 *
 * @author Ponfee
 * @see <a href="https://redisson.org">better implementation: redisson</a>
 */
public class RedisLock implements Lock {

    private static final Logger LOG = LoggerFactory.getLogger(RedisLock.class);

    /**
     * <pre>
     * KEYS[1]=key
     * ARGV[1]=pexpire milliseconds
     * ARGV[2]=lock value
     * </pre>
     *
     * Reentrant lock lua script
     */
    private static final String LOCK_LUA_SCRIPT =
        "if (   redis.call('exists',  KEYS[1]         )==0         \n" +
        "    or redis.call('hexists', KEYS[1], ARGV[2])==1  ) then \n" +
        "  redis.call('hincrby', KEYS[1], ARGV[2], 1);             \n" +
        "  redis.call('pexpire', KEYS[1], ARGV[1]);                \n" +
        "  return nil;                                             \n" +
        "end;                                                      \n" +
        "return redis.call('pttl', KEYS[1]);                       \n";

    /**
     * <pre>
     * KEYS[1]=key
     * ARGV[1]=pexpire milliseconds
     * ARGV[2]=lock value
     * </pre>
     *
     * <p>Unlock lua script
     */
    private static final String UNLOCK_LUA_SCRIPT =
        "if (redis.call('hexists', KEYS[1], ARGV[2]) == 0) then  \n" +
        "  return nil;                                           \n" +
        "end;                                                    \n" +
        "local cnt = redis.call('hincrby', KEYS[1], ARGV[2], -1) \n" +
        "if (cnt > 0) then                                       \n" +
        "  redis.call('pexpire', KEYS[1], ARGV[1]);              \n" +
        "  return 0;                                             \n" +
        "else                                                    \n" +
        "  redis.call('del', KEYS[1]);                           \n" +
        "  return 1;                                             \n" +
        "end;                                                    \n";

    /**
     * <pre>
     * KEYS[1]=key
     * ARGV[1]=pexpire milliseconds
     * ARGV[2]=lock value
     * </pre>
     *
     * <p>Renew lua script
     */
    private static final String RENEW_LUA_SCRIPT =
        "if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then \n" +
        "  redis.call('pexpire', KEYS[1], ARGV[1]);             \n" +
        "  return 1;                                            \n" +
        "end;                                                   \n" +
        "return 0;                                              \n";

    /**
     * Reentrant lock lua script
     */
    private static final RedisScript<Long> LOCK_SCRIPT = RedisScript.of(LOCK_LUA_SCRIPT, Long.class);

    /**
     * Unlock lua script
     */
    private static final RedisScript<Long> UNLOCK_SCRIPT = RedisScript.of(UNLOCK_LUA_SCRIPT, Long.class);

    /**
     * Spring redis template.
     */
    private final RedisTemplate<?, ?> redisTemplate;

    /**
     * Lock key
     */
    private final byte[] lockKey;

    /**
     * Lock uuid value
     */
    private final String lockUuid;

    /**
     * Lock timeout, prevent deadlock.
     */
    private final byte[] timeoutMillis;

    /**
     * Sleep millis.
     */
    private final long sleepMillis;

    /**
     * Constructor
     *
     * @param redisTemplate spring redis template
     * @param lockKey       the lock key
     * @param timeoutMillis lock timeout millis seconds(prevent deadlock)
     * @param sleepMillis   wait sleep milliseconds
     */
    RedisLock(RedisTemplate<?, ?> redisTemplate, String lockKey, long timeoutMillis, long sleepMillis) {
        Assert.notNull(redisTemplate, "Redis template cannot be null.");
        Assert.hasText(lockKey, "Lock key cannot be empty.");

        this.redisTemplate = redisTemplate;
        this.lockKey = ("lock:" + lockKey).getBytes(UTF_8);
        this.lockUuid = ObjectUtils.uuid32() + ":";
        this.timeoutMillis = Long.toString(timeoutMillis).getBytes(UTF_8);
        this.sleepMillis = Math.min(sleepMillis, timeoutMillis);
    }

    /**
     * 等待锁直到获取(non interrupt)
     */
    @Override
    public void lock() {
        for (int round = 0; !acquire(); round++) {
            try {
                Thread.sleep(computeSleepMillis(round));
            } catch (InterruptedException e) {
                LOG.error("Redis lock sleep occur interrupted exception.", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 等待锁直到获取成功或抛出InterruptedException异常
     *
     * @throws InterruptedException if call {@code Thread#interrupt}
     */
    @Override
    public void lockInterruptibly() throws InterruptedException {
        for (int round = 0; ; round++) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            if (acquire()) {
                break;
            }

            // To sleep for prevent endless loop
            Thread.sleep(computeSleepMillis(round));
        }
    }

    /**
     * 尝试获取锁，成功返回true，失败返回false
     *
     * @return if {@code true} get lock successfully
     */
    @Override
    public boolean tryLock() {
        return acquire();
    }

    /**
     * 尝试获取锁，成功返回true，失败返回false<br/>
     * 线程中断则抛出interrupted异常
     *
     * @param timeout the timeout value
     * @param unit    the timeout unit
     * @return {@code true} lock successfully
     * @throws InterruptedException if interrupted
     */
    @Override
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        long end = System.nanoTime() + unit.toNanos(timeout);
        for (; ; ) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            if (acquire()) {
                return true;
            }
            if (end < System.nanoTime()) {
                // 等待超时则返回
                return false;
            }
            Thread.sleep(sleepMillis);
        }
    }

    /**
     * 释放锁
     */
    @Override
    public void unlock() {
        release();
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException("Unsupported new condition operation.");
    }

    // ----------------------------------------------------------------------------------extra public methods

    /**
     * 当前线程是否持有锁
     * <pre>
     *  {@code
     *     class X {
     *       Lock lock = new RedisLock(redisTemplate, "lockKey", 100);
     *       // ...
     *       public void m() {
     *         assert !lock.isHeldByCurrentThread();
     *         lock.lock();
     *         try {
     *             // ... method body
     *         } finally {
     *             lock.unlock();
     *         }
     *       }
     *     }
     *  }
     * </pre>
     *
     * @return if {@code true} the current thread held locked
     */
    public boolean isHeldByCurrentThread() {
        return redisTemplate.execute((RedisCallback<byte[]>) conn -> conn.hGet(lockKey, getLockValue())) != null;
    }

    /**
     * 是否已锁（任何线程）
     *
     * @return {@code true} is locked
     */
    public boolean isLocked() {
        Boolean res = redisTemplate.execute((RedisCallback<Boolean>) conn -> conn.exists(lockKey));
        return res != null && res;
    }

    /**
     * Force unlock
     */
    public void forceUnlock() {
        redisTemplate.execute((RedisCallback<Long>) conn -> conn.del(lockKey));
    }

    // ---------------------------------------------------------------------------------------------private methods

    /**
     * Acquire lock
     *
     * @return {@code true} is acquired
     */
    private boolean acquire() {
        final byte[][] keysAndArgs = {lockKey, timeoutMillis, getLockValue()};

        // ret: null-获取锁成功；x-锁被其它线程持有(pttl的返回值)；
        Long ret = redisTemplate.execute((RedisCallback<Long>) conn -> {
            if (conn.isPipelined() || conn.isQueueing()) {
                throw new UnsupportedOperationException("Unsupported pipelined or queueing eval redis lua script.");
            }
            try {
                return conn.evalSha(LOCK_SCRIPT.getSha1(), ReturnType.INTEGER, 1, keysAndArgs);
            } catch (Exception e) {
                if (exceptionContainsNoScriptError(e)) {
                    LOG.info(e.getMessage());
                    return conn.eval(LOCK_SCRIPT.getScriptAsString().getBytes(UTF_8), ReturnType.INTEGER, 1, keysAndArgs);
                } else {
                    return ExceptionUtils.rethrow(e);
                }
            }
        });

        return ret == null;
    }

    /**
     * Release lock
     *
     * @return {@code true} is released
     */
    private boolean release() {
        final byte[][] keysAndArgs = {lockKey, timeoutMillis, getLockValue()};

        // ret: null-当前线程未持有锁；0-当前线程有重入且非最后一次释放锁；1-当前线程最后一次释放锁成功；
        Long ret = redisTemplate.execute((RedisCallback<Long>) conn -> {
            if (conn.isPipelined() || conn.isQueueing()) {
                // 在exec/closePipeline中会添加lua script sha1，所以这里只需要使用eval
                // return conn.eval(UNLOCK_SCRIPT.getScriptAsString().getBytes(UTF_8), ReturnType.INTEGER, 1, keysAndArgs);
                throw new UnsupportedOperationException("Unsupported pipelined or queueing eval redis lua script.");
            }
            try {
                return conn.evalSha(UNLOCK_SCRIPT.getSha1(), ReturnType.INTEGER, 1, keysAndArgs);
            } catch (Exception e) {
                if (exceptionContainsNoScriptError(e)) {
                    LOG.info(e.getMessage());
                    return conn.eval(UNLOCK_SCRIPT.getScriptAsString().getBytes(UTF_8), ReturnType.INTEGER, 1, keysAndArgs);
                } else {
                    return ExceptionUtils.rethrow(e);
                }
            }
        });

        return ret != null && ret == 1;
    }

    public static boolean exceptionContainsNoScriptError(Throwable e) {
        if (!(e instanceof NonTransientDataAccessException)) {
            return false;
        }

        Throwable current = e;
        while (current != null) {
            String exMessage = current.getMessage();
            if (exMessage != null && exMessage.contains("NOSCRIPT")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private byte[] getLockValue() {
        return (lockUuid + Thread.currentThread().getId()).getBytes(UTF_8);
    }

    private long computeSleepMillis(int round) {
        return round < 10 ? sleepMillis : Math.min(sleepMillis * round, 200);
    }

}
