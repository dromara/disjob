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

import cn.ponfee.disjob.common.spring.RedisKeyRenewal;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import cn.ponfee.disjob.dispatch.TaskDispatcher;
import cn.ponfee.disjob.registry.Discovery;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Dispatch task based redis
 *
 * @author Ponfee
 */
public class RedisTaskDispatcher extends TaskDispatcher {

    /**
     * <pre>
     * Worker renew map structure
     *  key: String type of workerRedisKey
     *  value: Renewer type of renew
     * </pre>
     */
    private final ConcurrentMap<String, RedisKeyRenewal> workerRenewMap = new ConcurrentHashMap<>();

    private final RedisTemplate<String, String> redisTemplate;

    public RedisTaskDispatcher(ApplicationEventPublisher eventPublisher,
                               Discovery<Worker> discoverWorker,
                               RetryProperties retryProperties,
                               RedisTemplate<String, String> redisTemplate,
                               @Nullable RedisTaskReceiver redisTaskReceiver) {
        super(eventPublisher, discoverWorker, retryProperties, redisTaskReceiver);

        this.redisTemplate = redisTemplate;
    }

    @Override
    protected final boolean doDispatch(ExecuteTaskParam param) {
        Worker worker = param.getWorker();
        // push to remote worker
        String key = RedisTaskDispatchingUtils.buildTaskDispatchKey(worker);
        // ret: return list length after call redis rpush command
        Long ret = redisTemplate.execute((RedisCallback<Long>) conn -> conn.rPush(key.getBytes(), param.serialize()));

        // renew redis key ttl
        workerRenewMap.computeIfAbsent(key, k -> new RedisKeyRenewal(redisTemplate, key)).renewIfNecessary();

        return (ret != null && ret > 0);
    }

}
