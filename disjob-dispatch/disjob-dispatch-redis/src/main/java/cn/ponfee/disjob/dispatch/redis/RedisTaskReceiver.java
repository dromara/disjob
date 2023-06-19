/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.dispatch.redis;

import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.common.lock.RedisLock;
import cn.ponfee.disjob.common.spring.RedisKeyRenewal;
import cn.ponfee.disjob.common.util.Collects;
import cn.ponfee.disjob.core.base.AbstractHeartbeatThread;
import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.param.ExecuteTaskParam;
import cn.ponfee.disjob.dispatch.TaskReceiver;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
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

    private final RedisTemplate<String, String> redisTemplate;
    private final List<GroupedWorker> gropedWorkers;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final ReceiveHeartbeatThread receiveHeartbeatThread;

    public RedisTaskReceiver(Worker currentWorker,
                             TimingWheel<ExecuteTaskParam> timingWheel,
                             RedisTemplate<String, String> redisTemplate) {
        super(timingWheel);

        this.redisTemplate = redisTemplate;
        this.gropedWorkers = Collects.convert(currentWorker.splitGroup(), GroupedWorker::new);
        this.receiveHeartbeatThread = new ReceiveHeartbeatThread(1000);
    }

    @Override
    public void start() {
        if (!started.compareAndSet(false, true)) {
            log.warn("Repeat call start method.");
            return;
        }
        this.receiveHeartbeatThread.start();
    }

    @Override
    public void stop() {
        if (!started.compareAndSet(true, false)) {
            log.warn("Repeat call stop method.");
            return;
        }
        this.receiveHeartbeatThread.close();
    }

    private class ReceiveHeartbeatThread extends AbstractHeartbeatThread {

        private ReceiveHeartbeatThread(long heartbeatPeriodMs) {
            super(heartbeatPeriodMs);
        }

        private boolean doReceive() {
            boolean isBusyLoop = true;

            for (GroupedWorker gropedWorker : gropedWorkers) {
                if (gropedWorker.skipNext) {
                    gropedWorker.skipNext = false;
                    continue;
                }
                List<byte[]> received = redisTemplate.execute((RedisCallback<List<byte[]>>) conn -> {
                    if (conn.isPipelined() || conn.isQueueing()) {
                        throw new UnsupportedOperationException("Unsupported pipelined or queueing redis operations.");
                    }

                    try {
                        return conn.evalSha(BATCH_POP_SCRIPT_SHA1, ReturnType.MULTI, 1, gropedWorker.keyAndArgs);
                    } catch (Exception e) {
                        if (RedisLock.exceptionContainsNoScriptError(e)) {
                            log.info(e.getMessage());
                            return conn.eval(BATCH_POP_SCRIPT_BYTES, ReturnType.MULTI, 1, gropedWorker.keyAndArgs);
                        } else {
                            log.error("Call redis eval sha occur error.", e);
                            return ExceptionUtils.rethrow(e);
                        }
                    }
                });

                gropedWorker.redisKeyRenewal.renewIfNecessary();

                if (CollectionUtils.isEmpty(received)) {
                    gropedWorker.skipNext = true;
                    continue;
                }

                for (byte[] bytes : received) {
                    ExecuteTaskParam param = ExecuteTaskParam.deserialize(bytes);
                    param.setWorker(gropedWorker.worker);
                    RedisTaskReceiver.this.receive(param);
                }

                if (received.size() < JobConstants.PROCESS_BATCH_SIZE) {
                    gropedWorker.skipNext = true;
                } else {
                    isBusyLoop = false;
                }
            }

            if (isBusyLoop) {
                // if busy loop, will be sleep heartbeat period milliseconds, so can't skip next task fetch
                gropedWorkers.forEach(e -> e.skipNext = false);
            }
            return isBusyLoop;
        }

        @Override
        protected boolean heartbeat() {
            return doReceive();
        }
    }

    private class GroupedWorker {
        private final Worker worker;
        private final byte[][] keyAndArgs;
        private final RedisKeyRenewal redisKeyRenewal;
        private volatile boolean skipNext = false;

        public GroupedWorker(Worker worker) {
            byte[] key = RedisTaskDispatchingUtils.buildDispatchTasksKey(worker).getBytes();
            this.worker = worker;
            this.keyAndArgs = new byte[][]{key, LIST_POP_BATCH_SIZE_BYTES};
            this.redisKeyRenewal = new RedisKeyRenewal(redisTemplate, key);
        }
    }

}
