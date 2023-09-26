///* __________              _____                                                *\
//** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
//**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
//**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
//**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
//**                      \/          \/     \/                                   **
//\*                                                                              */
//
//package cn.ponfee.disjob.supervisor.config;
//
//import cn.ponfee.disjob.common.util.NetUtils;
//import cn.ponfee.disjob.test.EmbeddedMysqlAndRedisServer;
//import org.apache.commons.lang3.tuple.Pair;
//import org.springframework.context.ConfigurableApplicationContext;
//import org.springframework.core.env.PropertiesPropertySource;
//import org.springframework.test.context.ContextConfigurationAttributes;
//import org.springframework.test.context.ContextCustomizer;
//import org.springframework.test.context.ContextCustomizerFactory;
//import org.springframework.test.context.MergedContextConfiguration;
//
//import java.util.List;
//import java.util.Properties;
//import java.util.concurrent.atomic.AtomicBoolean;
//
///**
// * <pre>
// * spring.factories
// *   org.springframework.test.context.ContextCustomizerFactory=\
// *   cn.ponfee.disjob.supervisor.config.InitializedContextCustomizerFactory
// * </pre>
// *
// * @author Ponfee
// */
//public class InitializedContextCustomizerFactory implements ContextCustomizerFactory {
//
//    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
//    private static Pair<String, String> ports;
//
//    @Override
//    public ContextCustomizer createContextCustomizer(Class<?> testClass, List<ContextConfigurationAttributes> configAttributes) {
//        return new InitializedContextCustomizer();
//    }
//
//    public static class InitializedContextCustomizer implements ContextCustomizer {
//        private static final String PROPERTY_NAME = "disjob.test.port";
//
//        @Override
//        public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
//            synchronized (InitializedContextCustomizer.class) {
//                if (INITIALIZED.compareAndSet(false, true)) {
//                    int mysqlPort = NetUtils.findAvailablePort(3306);
//                    int redisPort = NetUtils.findAvailablePort(6379);
//
//                    EmbeddedMysqlAndRedisServer.starter()
//                        .mysqlPort(mysqlPort)
//                        .redisMasterPort(redisPort)
//                        .redisSlavePort(NetUtils.findAvailablePort(6380))
//                        .start();
//                    ports = Pair.of(Integer.toString(mysqlPort), Integer.toString(redisPort));
//                }
//
//                Properties properties = new Properties();
//                properties.setProperty(PROPERTY_NAME + ".mysql", ports.getLeft());
//                properties.setProperty(PROPERTY_NAME + ".redis", ports.getRight());
//                PropertiesPropertySource propertiesPropertySource = new PropertiesPropertySource(PROPERTY_NAME, properties);
//                context.getEnvironment().getPropertySources().addFirst(propertiesPropertySource);
//            }
//        }
//    }
//
//}
