package cn.ponfee.scheduler.common.lock;

import cn.ponfee.scheduler.common.util.Numbers;
import cn.ponfee.scheduler.common.util.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.util.Assert;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Simply distributes lock based redis.
 *
 * @author Ponfee
 */
public class SimplyRedisLock implements java.io.Serializable {

    private static final long serialVersionUID = -8602109256331766412L;
    private static final Logger LOG = LoggerFactory.getLogger(SimplyRedisLock.class);

    /**
     * Redis SETNX(SET if Not eXists)
     */
    private static final byte[] NX_BYTES = RedisLock.NX.getBytes(UTF_8);

    /**
     * Redis PSETEX(SET and PeXpire)
     */
    private static final byte[] PX_BYTES = RedisLock.PX.getBytes(UTF_8);

    /**
     * Unlock lua script
     */
    private static final RedisScript<Long> UNLOCK_SCRIPT_OBJECT = new DefaultRedisScript<>(RedisLock.UNLOCK_SCRIPT_LUA, Long.class);

    /**
     * Lua script text byte array
     */
    private static final byte[] UNLOCK_SCRIPT_BYTES = UNLOCK_SCRIPT_OBJECT.getScriptAsString().getBytes(UTF_8);

    /**
     * Current thread locked value
     */
    private static final ThreadLocal<byte[]> LOCK_VALUE = new ThreadLocal<>();

    /**
     * Spring redis template.
     */
    private final transient RedisTemplate<?, ?> redisTemplate;

    /**
     * Lock timeout, prevent deadlock.
     */
    private final byte[] timeoutMillis;

    /**
     * Sleep millis.
     */
    private final long sleepMillis;

    public SimplyRedisLock(RedisTemplate<?, ?> redisTemplate) {
        this(redisTemplate, RedisLock.DEFAULT_TIMEOUT_MILLIS);
    }

    public SimplyRedisLock(RedisTemplate<?, ?> redisTemplate, int timeoutMillis) {
        this(redisTemplate, timeoutMillis, RedisLock.MIN_SLEEP_MILLIS);
    }

    /**
     * Constructor
     *
     * @param redisTemplate spring redis template
     * @param timeoutMillis redis timeout(prevent deadlock)
     * @param sleepMillis   wait sleep millis seconds
     */
    public SimplyRedisLock(RedisTemplate<?, ?> redisTemplate, int timeoutMillis, int sleepMillis) {
        Assert.notNull(redisTemplate, "Redis template cannot be null.");

        this.redisTemplate = redisTemplate;
        timeoutMillis = Numbers.bound(timeoutMillis, RedisLock.MIN_TIMEOUT_MILLIS, RedisLock.MAX_TIMEOUT_MILLIS);
        this.timeoutMillis = Long.toString(timeoutMillis).getBytes(UTF_8);
        this.sleepMillis = Numbers.bound(sleepMillis, RedisLock.MIN_SLEEP_MILLIS, timeoutMillis);
    }

    /**
     * 等待锁直到获取(non interrupt)
     * 
     * @param lockKey the lock key
     */
    public void lock(String lockKey) {
        for (int round = 0; !acquire(lockKey); round++) {
            try {
                TimeUnit.MILLISECONDS.sleep(computeSleepMillis(round));
            } catch (InterruptedException e) {
                LOG.error("Redis lock sleep occur interrupted exception.", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 等待锁直到获取成功或抛出InterruptedException异常
     *
     * @param lockKey the lock key
     * @throws InterruptedException if call {@code Thread#interrupt}
     */
    public void lockInterruptibly(String lockKey) throws InterruptedException {
        for (int round = 0; ; round++) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            if (acquire(lockKey)) {
                break;
            }

            // To sleep for prevent endless loop
            TimeUnit.MILLISECONDS.sleep(computeSleepMillis(round));
        }
    }

    /**
     * 尝试获取锁，成功返回true，失败返回false
     *
     * @param lockKey the lock key
     * @return if {@code true} get lock successfully
     */
    public boolean tryLock(String lockKey) {
        return acquire(lockKey);
    }

    /**
     * 尝试获取锁，成功返回true，失败返回false<br/>
     * 线程中断则抛出interrupted异常
     *
     * @param lockKey the lock key
     * @param timeout the timeout value
     * @param unit    the timeout unit
     * @return {@code true} lock successfully
     * @throws InterruptedException if interrupted
     */
    public boolean tryLock(String lockKey, long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        long end = System.nanoTime() + unit.toNanos(timeout);
        for (; ; ) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            if (acquire(lockKey)) {
                return true;
            }
            if (end < System.nanoTime()) {
                // 等待超时则返回
                return false;
            }
            TimeUnit.MILLISECONDS.sleep(sleepMillis);
        }
    }

    /**
     * 释放锁
     * 
     * @param lockKey the lock key
     */
    public void unlock(String lockKey) {
        release(lockKey);
    }

    // ----------------------------------------------------------------------------------extra public methods

    /**
     * 当前线程是否持有锁
     * <pre>
     *  {@code
     *     class X {
     *       Lock lock = new RedisLock(redisTemplate, 100);
     *       // ...
     *       public void m() {
     *         assert !lock.isHeldByCurrentThread(lockKey);
     *         lock.lock(lockKey);
     *         try {
     *             // ... method body
     *         } finally {
     *             lock.unlock(lockKey);
     *         }
     *       }
     *     }
     *  }
     * </pre>
     *
     * @param lockKey the lock key
     * @return if {@code true} the current thread held locked
     */
    public boolean isHeldByCurrentThread(String lockKey) {
        byte[] value = LOCK_VALUE.get();
        if (value == null) {
            return false;
        }
        return Arrays.equals(value, redisTemplate.execute((RedisCallback<byte[]>) conn -> conn.get(lockKey.getBytes(UTF_8))));
    }

    /**
     * 是否已锁（任何线程）
     *
     * @param lockKey the lock key
     * @return {@code true} is locked
     */
    public boolean isLocked(String lockKey) {
        Boolean res = redisTemplate.execute((RedisCallback<Boolean>) conn -> conn.exists(lockKey.getBytes(UTF_8)));
        return res != null && res;
    }

    /**
     * Force unlock
     * 
     * @param lockKey the lock key
     */
    public void funlock(String lockKey) {
        redisTemplate.execute((RedisCallback<Long>) conn -> conn.del(lockKey.getBytes(UTF_8)));
    }

    // ---------------------------------------------------------------------------------------------private methods

    /**
     * Acquire lock
     *
     * @param lockKey the lock key
     * @return {@code true} is acquired
     */
    private boolean acquire(String lockKey) {
        final byte[] lockValue = ObjectUtils.uuid(), lockKey0 = lockKey.getBytes(UTF_8);
        Boolean res = redisTemplate.execute((RedisCallback<Boolean>) conn -> {
            String ret = (String) conn.execute("SET", lockKey0, lockValue, NX_BYTES, PX_BYTES, timeoutMillis);
            if (RedisLock.SET_SUCCESS.equals(ret)) {
                LOCK_VALUE.set(lockValue);
                return true;
            } else {
                return false;
            }
        });
        return res != null && res;
    }

    /**
     * Release lock
     *
     * @param lockKey the lock key
     * @return {@code true} is released
     */
    private boolean release(String lockKey) {
        byte[] lockValue = LOCK_VALUE.get();
        if (lockValue == null) {
            return true;
        }

        final byte[][] keysAndArgs = {lockKey.getBytes(UTF_8), lockValue};
        Boolean res = redisTemplate.execute((RedisCallback<Boolean>) conn -> {
            if (conn.isPipelined() || conn.isQueueing()) {
                // 在exec/closePipeline中会添加lua script sha1，所以这里只需要使用eval
                conn.eval(UNLOCK_SCRIPT_BYTES, ReturnType.INTEGER, 1, keysAndArgs);
                return false;
            }

            Long ret;
            try {
                ret = conn.evalSha(UNLOCK_SCRIPT_OBJECT.getSha1(), ReturnType.INTEGER, 1, keysAndArgs);
            } catch (Exception e) {
                if (!RedisLock.exceptionContainsNoScriptError(e)) {
                    throw (e instanceof RuntimeException)
                        ? (RuntimeException) e
                        : new RedisSystemException(e.getMessage(), e);
                }
                ret = conn.eval(UNLOCK_SCRIPT_BYTES, ReturnType.INTEGER, 1, keysAndArgs);
            }
            return ret != null && ret == RedisLock.UNLOCK_SUCCESS;
        });

        LOCK_VALUE.remove();
        return res != null && res;
    }

    private long computeSleepMillis(int round) {
        return round < 15 ? sleepMillis : Math.min(sleepMillis * 30, 300);
    }

}
