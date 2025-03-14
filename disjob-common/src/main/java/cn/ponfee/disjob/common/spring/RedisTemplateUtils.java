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

import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingSupplier;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;

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

    public static <T> T evalScript(RedisTemplate<?, ?> redisTemplate,
                                   RedisScript<T> script, byte[][] keys, byte[]... args) {
        int numKeys = keys == null ? 0 : keys.length;
        return evalScript(redisTemplate, script, numKeys, Collects.concat(keys, args));
    }

    /**
     * <pre>
     * Execute lua script for redis
     *
     * 为了保证脚本里面的所有操作都在相同slot进行，云数据库Redis集群版本会对Lua脚本做如下限制：
     * 1、所有key都应该由KEYS数组来传递，脚本中执行命令`redis.call/pcall`的参数必须是KEYS[i]，不能使用本地变量（如`local key1 = KEY[i]; redis.call('get', key1)`）
     * 2、所有key必须在一个slot上，否则报错：ERR eval/evalsha command keys must be in same slot
     * 3、调用必须要带有key，否则报错：ERR for redis cluster, eval/evalsha number of keys can't be negative or zero
     *
     * redis hash tag: {commonTag}:1
     * </pre>
     *
     * @param redisTemplate the redis template
     * @param script        the lua script
     * @param numKeys       the number of keys
     * @param keysAndArgs   the keys and arguments
     * @param <T>           the return type
     * @return scrip executed result
     * @see RedisTemplate#execute(RedisScript, List, Object...)
     * @see RedisTemplate#execute(RedisScript, RedisSerializer, RedisSerializer, List, Object...)
     */
    public static <T> T evalScript(RedisTemplate<?, ?> redisTemplate,
                                   RedisScript<T> script, int numKeys, byte[][] keysAndArgs) {
        ReturnType returnType = ReturnType.fromJavaType(script.getResultType());
        return redisTemplate.execute((RedisCallback<T>) conn -> {
            if (conn.isPipelined() || conn.isQueueing()) {
                // We could script load first and then do evalsha to ensure sha is present,
                // but this adds a sha1 to exec/closePipeline results. Instead, just eval
                conn.eval(script.getScriptAsString().getBytes(UTF_8), returnType, numKeys, keysAndArgs);
                return null;
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

    public static RedisMessageListenerContainer createRedisMessageListenerContainer(StringRedisTemplate redisTemplate,
                                                                                    String channelTopicName,
                                                                                    Executor taskExecutor,
                                                                                    Object delegateObject,
                                                                                    String listenerMethodName) {
        // Check “void listenerMethod(String message, String channel)” method is valid
        ThrowingSupplier.doChecked(() -> delegateObject.getClass().getMethod(listenerMethodName, String.class, String.class));
        MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(delegateObject, listenerMethodName);
        listenerAdapter.afterPropertiesSet();

        return createRedisMessageListenerContainer(redisTemplate, channelTopicName, taskExecutor, listenerAdapter);
    }

    public static RedisMessageListenerContainer createRedisMessageListenerContainer(StringRedisTemplate redisTemplate,
                                                                                    String channelTopicName,
                                                                                    Executor taskExecutor,
                                                                                    MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(Objects.requireNonNull(redisTemplate.getConnectionFactory()));
        container.setTaskExecutor(taskExecutor);
        container.addMessageListener(listenerAdapter, new ChannelTopic(channelTopicName));
        container.afterPropertiesSet();
        container.start();
        return container;
    }

    public static String getServerInfo(RedisTemplate<?, ?> redisTemplate) {
        return getInfo(redisTemplate, "server");
    }

    public static String getInfo(RedisTemplate<?, ?> redisTemplate, String section) {
        return redisTemplate.execute((RedisConnection conn) -> Objects.toString(conn.info(section)));
    }

    private static boolean exceptionContainsNoScriptError(Throwable t) {
        if (!(t instanceof NonTransientDataAccessException)) {
            return false;
        }

        Set<Throwable> set = new HashSet<>();
        for (; t != null && set.add(t); t = t.getCause()) {
            String exMessage = t.getMessage();
            if (exMessage != null && exMessage.contains("NOSCRIPT")) {
                return true;
            }
        }
        return false;
    }

}
