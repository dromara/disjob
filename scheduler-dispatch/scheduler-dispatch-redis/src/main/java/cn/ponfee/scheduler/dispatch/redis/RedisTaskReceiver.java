/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.dispatch.redis;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.common.lock.RedisLock;
import cn.ponfee.scheduler.common.spring.RedisKeyRenewal;
import cn.ponfee.scheduler.core.base.AbstractHeartbeatThread;
import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.param.ExecuteTaskParam;
import cn.ponfee.scheduler.dispatch.TaskReceiver;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
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
    private static final byte[] LIST_POP_BATCH_SIZE_BYTES = Integer.toString(JobConstants.PROCESS_BATCH_SIZE).getBytes(UTF_8);

    private final Worker currentWorker;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisKeyRenewal redisKeyRenewal;
    private final byte[][] keysAndArgs;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final ReceiveHeartbeatThread receiveHeartbeatThread;

    public RedisTaskReceiver(Worker currentWorker,
                             TimingWheel<ExecuteTaskParam> timingWheel,
                             RedisTemplate<String, String> redisTemplate) {
        super(timingWheel);

        this.currentWorker = currentWorker;
        this.redisTemplate = redisTemplate;
        byte[] currentWorkerRedisKey = RedisTaskDispatchingUtils.buildDispatchTasksKey(currentWorker).getBytes();
        this.keysAndArgs = new byte[][]{currentWorkerRedisKey, LIST_POP_BATCH_SIZE_BYTES};
        this.redisKeyRenewal = new RedisKeyRenewal(redisTemplate, currentWorkerRedisKey);
        this.receiveHeartbeatThread = new ReceiveHeartbeatThread(1000);
    }

    @Override
    public void start() {
        if (!started.compareAndSet(false, true)) {
            LOG.warn("Repeat call start method.");
            return;
        }
        this.receiveHeartbeatThread.start();
    }

    @Override
    public void close() {
        this.receiveHeartbeatThread.close();
    }

    private boolean doReceive() {
        List<byte[]> result = redisTemplate.execute((RedisCallback<List<byte[]>>) conn -> {
            if (conn.isPipelined() || conn.isQueueing()) {
                throw new UnsupportedOperationException("Unsupported pipelined or queueing redis operations.");
            }

            try {
                return conn.evalSha(BATCH_POP_SCRIPT_SHA1, ReturnType.MULTI, 1, keysAndArgs);
            } catch (Exception e) {
                if (RedisLock.exceptionContainsNoScriptError(e)) {
                    LOG.info(e.getMessage());
                    return conn.eval(BATCH_POP_SCRIPT_BYTES, ReturnType.MULTI, 1, keysAndArgs);
                } else {
                    LOG.error("Call redis eval sha occur error.", e);
                    return ExceptionUtils.rethrow(e);
                }
            }
        });

        redisKeyRenewal.renewIfNecessary();

        if (result == null || result.isEmpty()) {
            return true;
        }

        for (byte[] bytes : result) {
            ExecuteTaskParam param = ExecuteTaskParam.deserialize(bytes);
            param.setWorker(currentWorker);
            super.receive(param);
        }

        return result.size() < JobConstants.PROCESS_BATCH_SIZE;
    }

    private class ReceiveHeartbeatThread extends AbstractHeartbeatThread {
        public ReceiveHeartbeatThread(long heartbeatPeriodMs) {
            super(heartbeatPeriodMs);
        }

        @Override
        protected boolean heartbeat() {
            return doReceive();
        }
    }

}
