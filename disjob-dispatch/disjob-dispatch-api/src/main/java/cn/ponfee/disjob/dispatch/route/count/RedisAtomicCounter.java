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

package cn.ponfee.disjob.dispatch.route.count;

import cn.ponfee.disjob.common.spring.RedisKeyRenewal;
import cn.ponfee.disjob.core.base.JobConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.function.Function;

/**
 * Atomic counter based redis INCRBY command.
 *
 * @author Ponfee
 */
public class RedisAtomicCounter extends AtomicCounter {

    private final String counterRedisKey;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisKeyRenewal redisKeyRenewal;

    /**
     * Function<String, AtomicCounter>: group -> new RedisAtomicCounter(group, stringRedisTemplate)
     *
     * @param group               the group
     * @param stringRedisTemplate the StringRedisTemplate
     * @see cn.ponfee.disjob.dispatch.route.RoundRobinExecutionRouter#RoundRobinExecutionRouter(Function)
     */
    public RedisAtomicCounter(String group,
                              StringRedisTemplate stringRedisTemplate) {
        this.counterRedisKey = JobConstants.DISJOB_KEY_PREFIX + ":route:counter:" + group;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisKeyRenewal = new RedisKeyRenewal(stringRedisTemplate, counterRedisKey);
    }

    @Override
    public long get() {
        String ret = stringRedisTemplate.opsForValue().get(counterRedisKey);
        if (StringUtils.isBlank(ret)) {
            return 0;
        }
        redisKeyRenewal.renewIfNecessary();
        return Long.parseLong(ret);
    }

    @Override
    public void set(long newValue) {
        stringRedisTemplate.opsForValue().set(counterRedisKey, Long.toString(newValue));
        redisKeyRenewal.renewIfNecessary();
    }

    @Override
    public long addAndGet(long delta) {
        Long value = stringRedisTemplate.opsForValue().increment(counterRedisKey, delta);
        redisKeyRenewal.renewIfNecessary();
        return value == null ? 0 : value;
    }

}
