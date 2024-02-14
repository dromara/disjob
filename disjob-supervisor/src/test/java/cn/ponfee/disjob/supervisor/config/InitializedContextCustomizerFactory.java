/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
