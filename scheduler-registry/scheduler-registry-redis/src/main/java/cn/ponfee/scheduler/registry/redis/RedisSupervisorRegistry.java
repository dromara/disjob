package cn.ponfee.scheduler.registry.redis;

import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Registry supervisor based redis.
 *
 * @author Ponfee
 */
public class RedisSupervisorRegistry extends RedisServerRegistry<Supervisor, Worker> implements SupervisorRegistry {

    public RedisSupervisorRegistry(String namespace, StringRedisTemplate stringRedisTemplate) {
        this(
            namespace,
            stringRedisTemplate,
            DEFAULT_REGISTRY_KEEP_ALIVE_MILLISECONDS,
            DEFAULT_DISCOVERY_REFRESH_INTERVAL_MILLISECONDS
        );
    }

    public RedisSupervisorRegistry(String namespace,
                                   StringRedisTemplate stringRedisTemplate,
                                   long keepAliveInMillis,
                                   long refreshIntervalMilliseconds) {
        super(namespace, stringRedisTemplate, keepAliveInMillis, refreshIntervalMilliseconds);
    }

}
