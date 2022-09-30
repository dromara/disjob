//package cn.ponfee.scheduler.common.util;
//
//import org.apache.commons.lang3.ClassUtils;
//import org.slf4j.LoggerFactory;
//
//import java.util.concurrent.TimeUnit;
//import java.util.function.Function;
//
///**
// * Bill data migrate metrics, based <a href="https://metrics.dropwizard.io/4.2.0/getting-started.html">metrics.dropwizard.io</a>
// *
// * <pre>
// * Gauges：用于提供自定义度量
// * Counters：计数器，本质是一个java.util.concurrent.atomic.LongAdder
// * Histograms：直方图数据
// * Meters：统计系统中某一事件的响应速率(如TPS、QPS)，该项指标值直接反应系统当前的处理能力
// * Timers：计时器，是Meter和Histogram的结合，可以统计接口请求速率和响应时长
// * </pre>
// *
// * @author Ponfee
// */
//public class Metrics {
//
//    public static final String BILL_MIGRATE_TPS_TAG = "bill.migrate.tps";
//
//    private static final MetricRegistry REGISTRY = new MetricRegistry();
//    private static final ScheduledReporter LOG_REPORTER = Slf4jReporter
//        .forRegistry(REGISTRY)
//        .outputTo(LoggerFactory.getLogger(ClassUtils.getPackageName(Metrics.class) + ".bill_migrate_metrics"))
//        .convertRatesTo(TimeUnit.SECONDS)
//        .convertDurationsTo(TimeUnit.MILLISECONDS)
//        .build();
//
//    static {
//        LOG_REPORTER.start(2, TimeUnit.MINUTES);
//    }
//
//    public static Named<Meter> meter(Class<?> klass, String... names) {
//        return metric(REGISTRY::meter, klass, names);
//    }
//
//    public static Named<Meter> meter(String name, String... names) {
//        return metric(REGISTRY::meter, name, names);
//    }
//
//    public static Named<Timer> timer(Class<?> klass, String... names) {
//        return metric(REGISTRY::timer, klass, names);
//    }
//
//    public static Named<Timer> timer(String name, String... names) {
//        return metric(REGISTRY::timer, name, names);
//    }
//
//    public static boolean remove(String name) {
//        return REGISTRY.remove(name);
//    }
//
//    public static boolean remove(Named<?> namedMetric) {
//        return REGISTRY.remove(namedMetric.name);
//    }
//
//    // ----------------------------------------------------------------private methods
//    private static <T extends Metric> Named<T> metric(Function<String, T> mapper,
//                                                      Class<?> klass,
//                                                      String... names) {
//        String metricName = MetricRegistry.name(klass, names);
//        return Named.of(metricName, mapper.apply(metricName));
//    }
//
//    private static <T extends Metric> Named<T> metric(Function<String, T> mapper,
//                                                      String name,
//                                                      String... names) {
//        String metricName = MetricRegistry.name(name, names);
//        return Named.of(metricName, mapper.apply(metricName));
//    }
//
//    public static class Named<T extends Metric> {
//        private final String name;
//        private final T metric;
//
//        private Named(String name, T metric) {
//            this.name = name;
//            this.metric = metric;
//        }
//
//        public static <T extends Metric> Named<T> of(String name, T metric) {
//            return new Named<>(name, metric);
//        }
//
//        public String name() {
//            return name;
//        }
//
//        public T metric() {
//            return metric;
//        }
//    }
//
//}
