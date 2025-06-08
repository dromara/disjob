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

package cn.ponfee.disjob.id.snowflake.zk;

import cn.ponfee.disjob.common.base.IdGenerator;
import cn.ponfee.disjob.common.spring.SpringUtils;
import cn.ponfee.disjob.id.snowflake.zk.ZkSnowflakeIdGenerator.ZkSnowFlakeWorkerRegistrar;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

import java.lang.annotation.*;

/**
 * ZkDistributedSnowflake IdGenerator spring configuration
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(ZkSnowFlakeWorkerRegistrar.class)
public @interface ZkSnowflakeIdGenerator {

    String curatorFrameworkRef() default "";

    String bizTag() default "disjob";

    int workerIdBitLength() default 8;

    int sequenceBitLength() default 14;

    /**
     * Basic ZkDistributedSnowflake
     */
    class BasicZkSnowflake extends ZkDistributedSnowflake {

        BasicZkSnowflake(CuratorFramework curatorFramework,
                         Object supervisor,
                         String bizTag,
                         int sequenceBitLength,
                         int workerIdBitLength) {
            super(curatorFramework, bizTag, serializeSupervisor(supervisor), sequenceBitLength, workerIdBitLength);
        }

        /**
         * Local supervisor spring bean name
         *
         * @see cn.ponfee.disjob.supervisor.configuration.EnableSupervisor.EnableSupervisorConfiguration#localSupervisor
         */
        static final String LOCAL_SUPERVISOR = "localSupervisor";

        /**
         * String of Supervisor.Local serialization
         *
         * @param supervisor the local supervisor
         * @return serialization string
         */
        private static String serializeSupervisor(Object supervisor) {
            String expectCls = "cn.ponfee.disjob.core.base.Supervisor$Local$1";
            String actualCls = supervisor.getClass().getName();
            Assert.isTrue(expectCls.equals(actualCls), () -> "Not a Supervisor$Local$1 instance: " + actualCls);
            return supervisor.toString();
        }
    }

    /**
     * Annotated ZkDistributedSnowflake
     */
    class AnnotatedZkSnowflake extends BasicZkSnowflake {

        AnnotatedZkSnowflake(@Autowired CuratorFramework curatorFramework, // use @Primary CuratorFramework bean
                             @Autowired @Qualifier(LOCAL_SUPERVISOR) Object supervisor,
                             String bizTag,
                             int sequenceBitLength,
                             int workerIdBitLength) {
            super(curatorFramework, supervisor, bizTag, sequenceBitLength, workerIdBitLength);
        }
    }

    class ZkSnowFlakeWorkerRegistrar implements ImportBeanDefinitionRegistrar {

        @SuppressWarnings("NullableProblems")
        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            ZkSnowflakeIdGenerator config = SpringUtils.parseAnnotation(ZkSnowflakeIdGenerator.class, importingClassMetadata);
            String curatorFrameworkRef = config.curatorFrameworkRef();
            GenericBeanDefinition bd;
            if (StringUtils.isBlank(curatorFrameworkRef)) {
                bd = new AnnotatedGenericBeanDefinition(AnnotatedZkSnowflake.class);
            } else {
                bd = new GenericBeanDefinition();
                bd.setBeanClass(BasicZkSnowflake.class);
                bd.getConstructorArgumentValues().addIndexedArgumentValue(0, new RuntimeBeanReference(curatorFrameworkRef));
                bd.getConstructorArgumentValues().addIndexedArgumentValue(1, new RuntimeBeanReference(BasicZkSnowflake.LOCAL_SUPERVISOR));
            }

            bd.getConstructorArgumentValues().addIndexedArgumentValue(2, config.bizTag());
            bd.getConstructorArgumentValues().addIndexedArgumentValue(3, config.workerIdBitLength());
            bd.getConstructorArgumentValues().addIndexedArgumentValue(4, config.sequenceBitLength());

            bd.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            bd.setScope(BeanDefinition.SCOPE_SINGLETON);
            registry.registerBeanDefinition(IdGenerator.class.getName(), bd);
        }
    }

}
