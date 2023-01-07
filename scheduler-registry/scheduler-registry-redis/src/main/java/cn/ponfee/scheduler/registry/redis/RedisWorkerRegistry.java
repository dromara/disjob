/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.registry.redis;

import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Registry worker based redis.
 *
 * @author Ponfee
 */
public class RedisWorkerRegistry extends RedisServerRegistry<Worker, Supervisor> implements WorkerRegistry {

    public RedisWorkerRegistry(String namespace, StringRedisTemplate stringRedisTemplate) {
        this(
            namespace,
            stringRedisTemplate,
            DEFAULT_REGISTRY_KEEP_ALIVE_MILLISECONDS,
            DEFAULT_DISCOVERY_REFRESH_INTERVAL_MILLISECONDS
        );
    }

    public RedisWorkerRegistry(String namespace,
                               StringRedisTemplate stringRedisTemplate,
                               long keepAliveInMillis,
                               long refreshIntervalMilliseconds) {
        super(namespace, stringRedisTemplate, keepAliveInMillis, refreshIntervalMilliseconds);
    }

}
