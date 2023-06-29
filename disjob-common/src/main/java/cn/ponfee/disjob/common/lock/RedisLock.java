/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.lock;

import cn.ponfee.disjob.common.util.Numbers;
import cn.ponfee.disjob.common.util.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.util.Assert;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Distributes lock based redis(unlock使用redis lua script)
 *
 * <pre>
 * class X {
 *   public void m() {
 *     Lock lock = new RedisLock(redisTemplate, "lockKey", 100);
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
 *     Lock lock = new RedisLock(redisTemplate, "lockKey", 100);
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
 *     Lock lock = new RedisLock(redisTemplate, "lockKey", 100);
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
public class RedisLock implements Lock, java.io.Serializable {

    private static final long serialVersionUID = 7019337086720416828L;
    private static final Logger LOG = LoggerFactory.getLogger(RedisLock.class);

    /**
     * Redis SET command return success message
     */
    private static final byte[] OK_MSG = "OK".getBytes(UTF_8);

    /**
     * Max timeout 1 day
     */
    private static final int MAX_TIMEOUT_MILLIS = 24 * 60 * 60 * 1000;

    /**
     * Default timeout 5 minutes
     */
    private static final int DEFAULT_TIMEOUT_MILLIS = 5 * 60 * 1000;

    /**
     * Min timeout 9 milliseconds
     */
    private static final int MIN_TIMEOUT_MILLIS = 9;

    /**
     * Min sleep 9 milliseconds
     */
    private static final int MIN_SLEEP_MILLIS = 9;

    /**
     * <pre>
     * KEYS[1]=key
     * ARGV[1]=value
     * ARGV[2]=milliseconds
     * ARGV[3]=value if current thread held lock
     * </pre>
     *
     * Reentrant lock lua script
     */
    private static final RedisScript<byte[]> LOCK_SCRIPT = new DefaultRedisScript<>(
        "local ret = redis.call('set', KEYS[1], ARGV[1], 'NX', 'PX', ARGV[2]);     \n" +
        "if (type(ret) == 'table') then                                            \n" +
        "  local e;                                                                \n" +
        "  for k,v in pairs(ret) do                                                \n" +
        "    e = v;                                                                \n" +
        "  end                                                                     \n" +
        "  if (e == 'OK') then                                                     \n" +
        "    return e;                                                             \n" +
        "  end                                                                     \n" +
        "end                                                                       \n" +
        "local val = redis.call('get', KEYS[1]);                                   \n" +
        "if (val == ARGV[3] and redis.call('pexpire', KEYS[1], ARGV[2]) ~= 1) then \n" +
        "  return nil;                                                             \n" +
        "end                                                                       \n" +
        "return val;                                                               \n",
        byte[].class
    );

    /**
     * <pre>
     * KEYS[1]=key
     * ARGV[1]=value if current thread held lock
     * </pre>
     *
     * Unlock lua script
     */
    private static final RedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
        "if (redis.call('get',KEYS[1]) == ARGV[1]) then \n" +
        "  return redis.call('del',KEYS[1]);            \n" +
        "else                                           \n" +
        "  return 0;                                    \n" +
        "end                                            \n",
        Long.class
    );

    /**
     * Current thread locked value
     */
    private static final ThreadLocal<byte[]> LOCK_VALUE = new ThreadLocal<>();

    /**
     * Spring redis template.
     */
    private final transient RedisTemplate<?, ?> redisTemplate;

    /**
     * Lock key
     */
    private final byte[] lockKey;

    /**
     * Lock timeout, prevent deadlock.
     */
    private final byte[] timeoutMillis;

    /**
     * Sleep millis.
     */
    private final long sleepMillis;

    public RedisLock(RedisTemplate<?, ?> redisTemplate, String lockKey) {
        this(redisTemplate, lockKey, DEFAULT_TIMEOUT_MILLIS);
    }

    public RedisLock(RedisTemplate<?, ?> redisTemplate, String lockKey, int timeoutMillis) {
        this(redisTemplate, lockKey, timeoutMillis, MIN_SLEEP_MILLIS);
    }

    /**
     * Constructor
     *
     * @param redisTemplate spring redis template
     * @param lockKey       the lock key
     * @param timeoutMillis lock timeout millis seconds(prevent deadlock)
     * @param sleepMillis   wait sleep millis seconds
     */
    public RedisLock(RedisTemplate<?, ?> redisTemplate, String lockKey, int timeoutMillis, int sleepMillis) {
        Assert.notNull(redisTemplate, "Redis template cannot be null.");
        Assert.hasText(lockKey, "Lock key cannot be empty.");

        this.redisTemplate = redisTemplate;
        // add key prefix "lock:"
        this.lockKey = ("lock:" + lockKey).getBytes(UTF_8);
        timeoutMillis = Numbers.bound(timeoutMillis, MIN_TIMEOUT_MILLIS, MAX_TIMEOUT_MILLIS);
        this.timeoutMillis = Long.toString(timeoutMillis).getBytes(UTF_8);
        this.sleepMillis = Numbers.bound(sleepMillis, MIN_SLEEP_MILLIS, timeoutMillis);
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
    public boolean tryLock(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
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
        byte[] value = LOCK_VALUE.get();
        if (value == null) {
            return false;
        }
        return Arrays.equals(value, redisTemplate.execute((RedisCallback<byte[]>) conn -> conn.get(lockKey)));
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
    public void funlock() {
        redisTemplate.execute((RedisCallback<Long>) conn -> conn.del(lockKey));
    }

    // ---------------------------------------------------------------------------------------------private methods

    /**
     * Acquire lock
     *
     * @return {@code true} is acquired
     */
    private boolean acquire() {
        final byte[] lockValue = ObjectUtils.uuid();

        /*
        String ret = redisTemplate.execute((RedisCallback<String>) conn ->
            (String) conn.execute("SET", lockKey, lockValue, "NX".getBytes(), "PX".getBytes(), timeoutMillis)
        );
        if ("OK".equals(ret)) {
            LOCK_VALUE.set(lockValue);
            return true;
        }
        return false;
        */

        byte[] currVal = LOCK_VALUE.get();
        final byte[][] keysAndArgs = {lockKey, lockValue, timeoutMillis, currVal};
        byte[] ret = redisTemplate.execute((RedisCallback<byte[]>) conn -> {
            if (conn.isPipelined() || conn.isQueueing()) {
                throw new UnsupportedOperationException("Unsupported pipelined or queueing eval redis lua script.");
            }
            try {
                return conn.evalSha(LOCK_SCRIPT.getSha1(), ReturnType.VALUE, 1, keysAndArgs);
            } catch (Exception e) {
                if (exceptionContainsNoScriptError(e)) {
                    LOG.info(e.getMessage());
                    return conn.eval(LOCK_SCRIPT.getScriptAsString().getBytes(UTF_8), ReturnType.VALUE, 1, keysAndArgs);
                } else {
                    return ExceptionUtils.rethrow(e);
                }
            }
        });
        if (ret == null) {
            return false;
        }
        if (Arrays.equals(ret, OK_MSG)) {
            LOCK_VALUE.set(lockValue);
            return true;
        }
        return Arrays.equals(ret, currVal);
    }

    /**
     * Release lock
     *
     * @return {@code true} is released
     */
    private boolean release() {
        byte[] lockValue = LOCK_VALUE.get();
        if (lockValue == null) {
            return true;
        }

        final byte[][] keysAndArgs = {lockKey, lockValue};
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

        // <key, value>为String时可使用此方法
        // Long ret = ((RedisTemplate<String, String>) redisTemplate).execute(UNLOCK_SCRIPT_OBJECT, Arrays.asList("key"), "val");

        LOCK_VALUE.remove();
        return ret != null && ret == 1L;
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

    private long computeSleepMillis(int round) {
        return round < 15 ? sleepMillis : Math.min(sleepMillis * 30, 300);
    }

}
