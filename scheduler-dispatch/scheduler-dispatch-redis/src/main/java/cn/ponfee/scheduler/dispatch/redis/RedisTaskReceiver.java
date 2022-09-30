package cn.ponfee.scheduler.dispatch.redis;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.scheduler.common.lock.RedisLock;
import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.dispatch.DispatchConstants;
import cn.ponfee.scheduler.dispatch.TaskReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Task receiver based redis.
 *
 * @author Ponfee
 */
public class RedisTaskReceiver extends TaskReceiver {

    private static final Logger LOG = LoggerFactory.getLogger(RedisTaskReceiver.class);

    /**
     * List Batch pop lua script
     *
     * <pre>{@code
     *   // 1、获取[0 ~ n-1]之间的元素
     *   lrange(key, 0, n-1)
     *
     *   // 2、保留[n ~ -1]之间的元素
     *   ltrim(key, n, -1)
     * }</pre>
     */
    private static final RedisScript<List> BATCH_POP_SCRIPT_OBJECT = new DefaultRedisScript<>(
        "local ret=redis.call('lrange',KEYS[1],0,ARGV[1]-1);redis.call('ltrim',KEYS[1],ARGV[1],-1);return ret;", List.class
    );

    /**
     * Redis lua script sha1
     */
    private static final String BATCH_POP_SCRIPT_SHA1 = BATCH_POP_SCRIPT_OBJECT.getSha1();

    /**
     * Lua script text byte array
     */
    private static final byte[] BATCH_POP_SCRIPT_BYTES = BATCH_POP_SCRIPT_OBJECT.getScriptAsString().getBytes(UTF_8);

    /**
     * List left pop batch size
     */
    private static final byte[] LIST_POP_BATCH_SIZE_BYTES = Integer.toString(200).getBytes(UTF_8);

    private final Worker currentWorker;
    private final RedisTemplate<String, String> redisTemplate;
    private final byte[] currentWorkerRedisKey;
    private final ScheduledThreadPoolExecutor receiveTaskScheduledExecutor;
    private final AtomicBoolean start = new AtomicBoolean(false);

    public RedisTaskReceiver(Worker currentWorker,
                             TimingWheel timingWheel,
                             RedisTemplate<String, String> redisTemplate) {
        super(timingWheel);

        this.currentWorker = currentWorker;
        this.redisTemplate = redisTemplate;
        this.currentWorkerRedisKey = DispatchConstants.buildDispatchTasksKey(currentWorker).getBytes();
        this.receiveTaskScheduledExecutor = new ScheduledThreadPoolExecutor(1, ThreadPoolExecutors.DISCARD);
    }

    @Override
    public void start() {
        if (!start.compareAndSet(false, true)) {
            return;
        }
        receiveTaskScheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                doReceive();
            } catch (Exception e) {
                LOG.error("Redis task receive scheduled error.", e);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        receiveTaskScheduledExecutor.shutdownNow();
    }

    private void doReceive() {
        final byte[][] keysAndArgs = {currentWorkerRedisKey, LIST_POP_BATCH_SIZE_BYTES};
        List<byte[]> result = redisTemplate.execute((RedisCallback<List<byte[]>>) conn -> {
            if (conn.isPipelined() || conn.isQueueing()) {
                throw new UnsupportedOperationException("Unsupported pipelined or queueing redis operations.");
            }

            try {
                return conn.evalSha(BATCH_POP_SCRIPT_SHA1, ReturnType.MULTI, 1, keysAndArgs);
            } catch (Exception e) {
                LOG.warn("Call redis eval sha occur error.", e);
                if (!RedisLock.exceptionContainsNoScriptError(e)) {
                    throw (e instanceof RuntimeException)
                        ? (RuntimeException) e
                        : new RedisSystemException(e.getMessage(), e);
                }
                return conn.eval(BATCH_POP_SCRIPT_BYTES, ReturnType.MULTI, 1, keysAndArgs);
            }
        });

        try {
            redisTemplate.execute((RedisCallback<?>) conn -> conn.expire(currentWorkerRedisKey, JobConstants.REDIS_KEY_TTL_SECONDS));
        } catch (Exception e) {
            LOG.warn("Renew dispatch worker key occur error.", e);
        }

        if (result == null || result.isEmpty()) {
            return;
        }

        for (byte[] bytes : result) {
            ExecuteParam executeParam = ExecuteParam.deserialize(bytes);
            executeParam.setWorker(currentWorker);
            super.receive(executeParam);
        }
    }

}
