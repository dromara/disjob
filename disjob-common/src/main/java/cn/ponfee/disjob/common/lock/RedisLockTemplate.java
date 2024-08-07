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

package cn.ponfee.disjob.common.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * Lock template based redis.
 *
 * @author Ponfee
 */
public class RedisLockTemplate implements LockTemplate {

    private static final Logger LOG = LoggerFactory.getLogger(RedisLockTemplate.class);

    private final RedisLock redisLock;

    public RedisLockTemplate(RedisLock redisLock) {
        this.redisLock = redisLock;
    }

    @Override
    public <T> T execute(Callable<T> action) {
        if (redisLock.tryLock()) {
            try {
                return action.call();
            } catch (Throwable t) {
                LOG.error("Executed in redis lock occur error.", t);
                return null;
            } finally {
                redisLock.unlock();
            }
        } else {
            return null;
        }
    }
}
