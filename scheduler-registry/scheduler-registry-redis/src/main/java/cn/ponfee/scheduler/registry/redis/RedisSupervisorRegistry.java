package cn.ponfee.scheduler.registry.redis;

import cn.ponfee.scheduler.common.base.DoubleListViewer;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Registry supervisor based redis.
 *
 * @author Ponfee
 */
public class RedisSupervisorRegistry extends RedisServerRegistry<Supervisor, Worker> implements SupervisorRegistry {

    private volatile Map<String, List<Worker>> groupedWorkers;
    private volatile List<Worker> allWorkers;

    public RedisSupervisorRegistry(StringRedisTemplate stringRedisTemplate) {
        this(
            stringRedisTemplate,
            DEFAULT_REGISTRY_KEEP_ALIVE_MILLISECONDS,
            DEFAULT_DISCOVERY_REFRESH_INTERVAL_MILLISECONDS
        );
    }

    public RedisSupervisorRegistry(StringRedisTemplate stringRedisTemplate,
                                   long keepAliveInMillis,
                                   long refreshIntervalMilliseconds) {
        super(stringRedisTemplate, keepAliveInMillis, refreshIntervalMilliseconds);
    }

    @Override
    protected List<Worker> getServers0(String group) {
        return group == null ? allWorkers : groupedWorkers.get(group);
    }

    @Override
    protected void refreshDiscoveryServers(List<Worker> discoveredWorkers) {
        if (CollectionUtils.isEmpty(discoveredWorkers)) {
            this.groupedWorkers = Collections.emptyMap();
            this.allWorkers = Collections.emptyList();
        } else {
            Map<String, List<Worker>> map = SupervisorRegistry.groupByWorkers(discoveredWorkers);
            DoubleListViewer<Worker> list = new DoubleListViewer<>(map.values());
            this.groupedWorkers = map;
            this.allWorkers = list;
        }
    }

    @Override
    public void close() {
        super.close();
        this.groupedWorkers = null;
        this.allWorkers = null;
    }

}
