/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

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
 *
 * @author Ponfee
 */
public class RedisTemplateUtils {

    private static final Logger LOG = LoggerFactory.getLogger(RedisTemplateUtils.class);

    /**
     * Execute lua script for redis
     *
     * @param redisTemplate the redis template
     * @param script        the lua script
     * @param returnType    the return type
     * @param numKeys       the number of keys
     * @param keysAndArgs   the keys and arguments
     * @param <T>           the return type
     * @return scrip executed result
     */
    public static <T> T executeScript(RedisTemplate<?, ?> redisTemplate,
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
