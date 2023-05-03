/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry.redis;

import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.registry.redis.configuration.RedisRegistryProperties;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Registry supervisor based redis.
 *
 * @author Ponfee
 */
public class RedisSupervisorRegistry extends RedisServerRegistry<Supervisor, Worker> implements SupervisorRegistry {

    public RedisSupervisorRegistry(StringRedisTemplate stringRedisTemplate, RedisRegistryProperties config) {
        super(stringRedisTemplate, config);
    }

}
