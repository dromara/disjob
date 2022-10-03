package cn.ponfee.scheduler.registry.redis;

import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Registry worker based redis.
 *
 * @author Ponfee
 */
public class RedisWorkerRegistry extends RedisServerRegistry<Worker, Supervisor> implements WorkerRegistry {

    private volatile List<Supervisor> supervisors = Collections.emptyList();

    public RedisWorkerRegistry(StringRedisTemplate stringRedisTemplate) {
        this(
            stringRedisTemplate,
            DEFAULT_REGISTRY_KEEP_ALIVE_MILLISECONDS,
            DEFAULT_DISCOVERY_REFRESH_INTERVAL_MILLISECONDS
        );
    }

    public RedisWorkerRegistry(StringRedisTemplate stringRedisTemplate,
                               long keepAliveInMillis,
                               long refreshIntervalMilliseconds) {
        super(stringRedisTemplate, keepAliveInMillis, refreshIntervalMilliseconds);
    }

    @Override
    protected List<Supervisor> getServers(String group, boolean forceRefresh) {
        Assert.isNull(group, "Supervisor non grouped, group must be null.");
        doRefreshDiscoveryInSynchronized(servers -> {
            List<Supervisor> discoveredSupervisors = servers.stream()
                                                            .map(Supervisor::deserialize)
                                                            .collect(Collectors.toList());
            List<Supervisor> aliveSupervisors = filterAvailableServers(discoveredSupervisors);
            if (aliveSupervisors.isEmpty()) {
                logger.warn("All discovered supervisors not available.");
                List<Supervisor> list = this.supervisors.stream()
                                                        .filter(e -> !discoveredSupervisors.contains(e))
                                                        .collect(Collectors.toList());
                aliveSupervisors = filterAvailableServers(list);
            }

            if (aliveSupervisors.isEmpty()) {
                logger.error("Not found available supervisors.");
                this.supervisors = Collections.emptyList();
            } else {
                // Sort for help use route supervisor
                aliveSupervisors.sort(Comparator.comparing(Supervisor::getHost));
                this.supervisors = Collections.unmodifiableList(aliveSupervisors);
            }
        }, forceRefresh);

        return supervisors;
    }

}
