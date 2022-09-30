//package cn.ponfee.scheduler.common.util;
//
//import cn.huolala.bme.common.cache.PeriodRefreshRedisCache;
//import com.google.common.util.concurrent.RateLimiter;
//import lombok.Data;
//import org.apache.commons.collections4.MapUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Component;
//
//import java.io.Serializable;
//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.util.Optional;
//import java.util.SortedMap;
//import java.util.TreeMap;
//import java.util.concurrent.locks.Lock;
//import java.util.concurrent.locks.ReentrantLock;
//
///**
// * Flow controller
// *
// * <pre>
// * {
// *   "sleep":5000,
// *   "limit":{
// *     "00":39,
// *     "04":32,
// *     "08":44,
// *     "12":52,
// *     "16":40,
// *     "20":32
// *   }
// * }
// * </pre>
// *
// * @author Ponfee
// */
//@Component
//public class FlowController {
//
//    private static final Logger LOG = LoggerFactory.getLogger(FlowController.class);
//
//    private static final String REDIS_KEY = "migrate_bill_flow_control_config";
//    private static final int REFRESH_TIME_MILLIS = 15000;
//
//    private final Lock refreshLock = new ReentrantLock();
//    private final PeriodRefreshRedisCache<String, String> redisCache;
//
//    private final RateLimiter rateLimiter = RateLimiter.create(1, Duration.ofSeconds(120));
//    private volatile long sleepMillis = 5000L;
//
//    private volatile long nextRefreshTimeMillis = 0L;
//    private volatile int round = 0;
//
//    public FlowController(PeriodRefreshRedisCache<String, String> cache) {
//        this.redisCache = cache;
//    }
//
//    public void control(int permits) throws Exception {
//        refresh();
//
//        final long localSleepMillis = sleepMillis;
//        if (localSleepMillis > 0) {
//            Thread.sleep(localSleepMillis);
//        }
//
//        double spentSleeping = rateLimiter.acquire(permits);
//
//        if (++round == 43) {
//            // 只用于日志打印，不要求原子性
//            round = 0;
//            LOG.info("Rate limit spend sleep time seconds: {}", Numbers.format(spentSleeping));
//        }
//    }
//
//    private void refresh() {
//        long currentTimeMillis = System.currentTimeMillis();
//        if (currentTimeMillis < nextRefreshTimeMillis) {
//            return;
//        }
//
//        if (!refreshLock.tryLock()) {
//            return;
//        }
//        try {
//            String text = null;
//            Optional<Config> config;
//            try {
//                text = redisCache.get(REDIS_KEY);
//                config = Optional.ofNullable(Jsons.fromJson(text, Config.class));
//            } catch (Exception e) {
//                LOG.error("Parse flow control config failed: " + text, e);
//                config = Optional.empty();
//            }
//
//            // refresh limit config
//            Integer limitConfig = config.map(Config::latestLimit).orElse(null);
//            if (limitConfig != null) {
//                if (limitConfig < 1) {
//                    LOG.error("Invalid limit config {}", limitConfig);
//                } else if (Math.abs(limitConfig - rateLimiter.getRate()) > 0.5D) {
//                    LOG.info("Refresh limit config from {} to {}", rateLimiter.getRate(), limitConfig);
//                    rateLimiter.setRate(limitConfig);
//                }
//            }
//
//            // refresh sleep config
//            Long sleepConfig = config.map(Config::getSleep).orElse(null);
//            if (sleepConfig != null) {
//                if (sleepConfig < 0) {
//                    LOG.error("Invalid sleep config {}", limitConfig);
//                } else if (sleepMillis != sleepConfig) {
//                    LOG.info("Refresh sleep config from {} to {}", sleepMillis, sleepConfig);
//                    sleepMillis = sleepConfig;
//                }
//            }
//
//            this.nextRefreshTimeMillis = currentTimeMillis + REFRESH_TIME_MILLIS;
//        } finally {
//            refreshLock.unlock();
//        }
//    }
//
//    @Data
//    public static class Config implements Serializable {
//        private static final long serialVersionUID = 3775843572198792366L;
//        private final long sleep;
//        private final TreeMap<String, Integer> limit;
//
//        public Config(long sleep, TreeMap<String, Integer> limit) {
//            this.sleep = sleep;
//            this.limit = limit;
//        }
//
//        public Integer latestLimit() {
//            if (MapUtils.isEmpty(limit)) {
//                return null;
//            }
//
//            String hourOfDay = String.format("%02d", LocalDateTime.now().getHour());
//
//            Integer value = limit.get(hourOfDay);
//            if (value != null) {
//                return value;
//            }
//
//            SortedMap<String, Integer> headMap = limit.headMap(hourOfDay);
//            return headMap.isEmpty() ? limit.firstEntry().getValue() : limit.get(headMap.lastKey());
//        }
//    }
//
//}
