/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.dispatch.redis;

import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.common.base.TripState;
import cn.ponfee.disjob.common.concurrent.AbstractHeartbeatThread;
import cn.ponfee.disjob.common.spring.RedisKeyRenewal;
import cn.ponfee.disjob.common.spring.RedisTemplateUtils;
import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import cn.ponfee.disjob.dispatch.TaskReceiver;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Task receiver based redis.
 *
 * <p>
 * <pre>Redis list queue: {@code
 * lrange list_queue 0 -1
 *
 * rpush list_queue a b c d e f g
 * lrange list_queue 0 -1
 *
 * lrange list_queue 0 2
 * lrange list_queue 0 -1
 *
 * ltrim  list_queue 3 -1
 * lrange list_queue 0 -1
 * }</pre>
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
    private static final RedisScript<List> BATCH_POP_SCRIPT = RedisScript.of(
        "local ret = redis.call('lrange', KEYS[1], 0, ARGV[1]-1); \n" +
        "redis.call('ltrim', KEYS[1], ARGV[1], -1);               \n" +
        "return ret;                                              \n" ,
        List.class
    );

    /**
     * List left pop batch size
     */
    private static final byte[] LIST_POP_BATCH_SIZE_BYTES = Integer.toString(JobConstants.PROCESS_BATCH_SIZE).getBytes(UTF_8);

    private final TripState state = TripState.create();
    private final ReceiveHeartbeatThread receiveHeartbeatThread;

    public RedisTaskReceiver(Worker.Current currentWorker,
                             TimingWheel<ExecuteTaskParam> timingWheel,
                             RedisTemplate<String, String> redisTemplate) {
        super(currentWorker, timingWheel);

        SingletonClassConstraint.constrain(this);
        this.receiveHeartbeatThread = new ReceiveHeartbeatThread(1000, redisTemplate, currentWorker, this);
    }

    @Override
    public boolean receive(ExecuteTaskParam task) {
        return super.doReceive(task);
    }

    @Override
    public void start() {
        if (!state.start()) {
            log.warn("Repeat call start method.");
            return;
        }
        this.receiveHeartbeatThread.start();
    }

    @Override
    public void stop() {
        if (!state.stop()) {
            log.warn("Repeat call stop method.");
            return;
        }
        this.receiveHeartbeatThread.close();
    }

    private static class ReceiveHeartbeatThread extends AbstractHeartbeatThread {
        private final RedisTemplate<String, String> redisTemplate;
        private final GroupedWorker gropedWorker;
        private final RedisTaskReceiver redisTaskReceiver;

        private ReceiveHeartbeatThread(long heartbeatPeriodMs,
                                       RedisTemplate<String, String> redisTemplate,
                                       Worker currentWorker,
                                       RedisTaskReceiver redisTaskReceiver) {
            super(heartbeatPeriodMs);
            this.redisTemplate = redisTemplate;
            this.gropedWorker = new GroupedWorker(currentWorker, redisTemplate);
            this.redisTaskReceiver = redisTaskReceiver;
        }

        @Override
        protected boolean heartbeat() {
            List<byte[]> received = RedisTemplateUtils.evalScript(redisTemplate, BATCH_POP_SCRIPT, 1, gropedWorker.keysAndArgs);
            gropedWorker.redisKeyRenewal.renewIfNecessary();
            if (CollectionUtils.isEmpty(received)) {
                return true;
            }
            for (byte[] bytes : received) {
                redisTaskReceiver.receive(ExecuteTaskParam.deserialize(bytes));
            }
            return received.size() < JobConstants.PROCESS_BATCH_SIZE;
        }
    }

    private static class GroupedWorker {
        private final byte[][] keysAndArgs;
        private final RedisKeyRenewal redisKeyRenewal;

        private GroupedWorker(Worker worker, RedisTemplate<String, String> redisTemplate) {
            byte[] key = RedisTaskDispatchingUtils.buildDispatchTasksKey(worker).getBytes();
            this.keysAndArgs = new byte[][]{key, LIST_POP_BATCH_SIZE_BYTES};
            this.redisKeyRenewal = new RedisKeyRenewal(redisTemplate, key);
        }
    }

}
