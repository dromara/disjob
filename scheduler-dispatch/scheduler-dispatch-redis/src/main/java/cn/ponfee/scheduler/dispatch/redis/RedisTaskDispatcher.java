package cn.ponfee.scheduler.dispatch.redis;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.common.spring.RedisKeyRenewal;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.dispatch.TaskDispatcher;
import cn.ponfee.scheduler.registry.Discovery;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dispatch task based redis
 *
 * @author Ponfee
 */
public class RedisTaskDispatcher extends TaskDispatcher {

    /**
     * <pre>
     * Worker renew map structure
     *  key: String type of workerRedisKey
     *  value: Renewer type of renew
     * </pre>
     */
    private final Map<String, RedisKeyRenewal> workerRenewMap = new ConcurrentHashMap<>();

    private final RedisTemplate<String, String> redisTemplate;

    public RedisTaskDispatcher(Discovery<Worker> discoveryWorker,
                               @Nullable TimingWheel<ExecuteParam> timingWheel,
                               RedisTemplate<String, String> redisTemplate) {
        super(discoveryWorker, timingWheel);
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected final boolean dispatch(ExecuteParam executeParam) {
        Worker worker = executeParam.getWorker();
        // push to remote worker
        String key = RedisTaskDispatchingUtils.buildDispatchTasksKey(worker);
        // ret: return list length after call redis rpush command
        Long ret = redisTemplate.execute(
            (RedisCallback<Long>) conn -> conn.rPush(key.getBytes(), executeParam.serialize())
        );

        RedisKeyRenewal renewal = workerRenewMap.computeIfAbsent(key, k -> new RedisKeyRenewal(redisTemplate, key));
        renewal.renewIfNecessary();

        return (ret != null && ret > 0);
    }

}
