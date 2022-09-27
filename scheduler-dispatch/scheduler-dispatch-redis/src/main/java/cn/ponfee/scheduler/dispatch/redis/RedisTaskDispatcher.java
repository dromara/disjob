package cn.ponfee.scheduler.dispatch.redis;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.core.redis.RedisKeyUtils;
import cn.ponfee.scheduler.dispatch.TaskDispatcher;
import cn.ponfee.scheduler.registry.Discovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Dispatch task based redis
 *
 * @author Ponfee
 */
public class RedisTaskDispatcher extends TaskDispatcher {

    private final static Logger LOG = LoggerFactory.getLogger(RedisTaskDispatcher.class);

    /**
     * <pre>
     * Worker renew map structure
     *  key: String type of workerRedisKey
     *  value: Renewer type of renew
     * </pre>
     */
    private final Map<String, Renewal> workerRenewMap = new ConcurrentHashMap<>();

    private final RedisTemplate<String, String> redisTemplate;

    public RedisTaskDispatcher(RedisTemplate<String, String> redisTemplate,
                               Discovery<Worker> discoveryWorker,
                               TimingWheel<ExecuteParam> timingWheel) {
        super(discoveryWorker, timingWheel);
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected boolean dispatch(ExecuteParam executeParam) {
        Worker worker = executeParam.getWorker();
        // push to remote worker
        String key = RedisKeyUtils.buildDispatchTasksKey(worker);
        // ret: return list length after call redis rpush command
        Long ret = redisTemplate.execute(
            (RedisCallback<Long>) conn -> conn.rPush(key.getBytes(), executeParam.serialize())
        );
        renewIfNecessary(worker, key);
        return (ret != null && ret > 0);
    }

    /**
     * Renew the worker redis key expire
     *
     * @param worker    the worker
     * @param workerKey the worker key
     */
    private void renewIfNecessary(Worker worker, String workerKey) {
        Renewal renewal = workerRenewMap.computeIfAbsent(workerKey, k -> new Renewal());
        if (renewal.requireRenew()) {
            return;
        }

        synchronized (renewal) {
            if (renewal.requireRenew()) {
                return;
            }

            renewal.renew(workerKey);
            LOG.info("Worker redis key renewed {}", worker.toString());
        }
    }

    /**
     * Renew redis key ttl
     */
    private class Renewal {
        private volatile long nextRenewTimeMillis = 0;

        private void renew(String workerKey) {
            redisTemplate.expire(workerKey, RedisKeyUtils.REDIS_KEY_TTL_SECONDS, TimeUnit.SECONDS);
            this.nextRenewTimeMillis = System.currentTimeMillis() + RedisKeyUtils.REDIS_KEY_TTL_RENEW_INTERVAL_MILLIS;
        }

        private boolean requireRenew() {
            return System.currentTimeMillis() < nextRenewTimeMillis;
        }
    }
}
