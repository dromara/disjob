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

package cn.ponfee.disjob.common.spring;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Redis template utils
 * <p>redis.call：抛出异常给调用方
 * <p>redis.pcall：捕获异常并以Lua表的形式返回给调用方
 *
 * @author Ponfee
 */
public class RedisTemplateUtils {

    private static final Logger LOG = LoggerFactory.getLogger(RedisTemplateUtils.class);

    /**
     * <pre>
     * Execute lua script for redis
     *
     * 为了保证脚本里面的所有操作都在相同slot进行，云数据库Redis集群版本会对Lua脚本做如下限制：
     * 1、所有key都应该由KEYS数组来传递，脚本中执行命令`redis.call/pcall`的参数必须是KEYS[i]，不能使用本地变量（如`local key1 = KEY[i]; redis.call('get', key1)`）
     * 2、所有key必须在一个slot上，否则报错：ERR eval/evalsha command keys must be in same slot
     * 3、调用必须要带有key，否则报错：ERR for redis cluster, eval/evalsha number of keys can't be negative or zero
     * </pre>
     *
     * @param redisTemplate the redis template
     * @param script        the lua script
     * @param returnType    the return type
     * @param numKeys       the number of keys
     * @param keysAndArgs   the keys and arguments
     * @param <T>           the return type
     * @return scrip executed result
     */
    public static <T> T evalScript(RedisTemplate<?, ?> redisTemplate,
                                   RedisScript<T> script, ReturnType returnType, int numKeys, byte[][] keysAndArgs) {
        return redisTemplate.execute((RedisCallback<T>) conn -> {
            if (conn.isPipelined() || conn.isQueueing()) {
                // 在exec/closePipeline中会添加lua script sha1，所以这里只需要使用eval
                // return conn.eval(script.getScriptAsString().getBytes(UTF_8), returnType, numKeys, keysAndArgs);
                throw new UnsupportedOperationException("Unsupported pipelined or queueing eval redis lua script.");
            }
            try {
                return conn.evalSha(script.getSha1(), returnType, numKeys, keysAndArgs);
            } catch (Exception e) {
                if (exceptionContainsNoScriptError(e)) {
                    LOG.info(e.getMessage());
                    return conn.eval(script.getScriptAsString().getBytes(UTF_8), returnType, numKeys, keysAndArgs);
                } else {
                    return ExceptionUtils.rethrow(e);
                }
            }
        });
    }

    private static boolean exceptionContainsNoScriptError(Throwable e) {
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

}
