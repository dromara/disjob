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

package cn.ponfee.disjob.id.snowflake.db;

import cn.ponfee.disjob.common.base.IdGenerator;
import cn.ponfee.disjob.common.spring.SpringUtils;
import cn.ponfee.disjob.id.snowflake.db.DbSnowflakeIdGenerator.DbSnowFlakeWorkerRegistrar;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.*;

/**
 * <pre>
 * DbSnowflake IdGenerator spring configuration
 *
 * `@Import常见的6种用法
 *   1）普通类：bean名称为完整的类名
 *   2）@Configuration标注的类
 *   3）@ComponentScan标注的类
 *   4）ImportBeanDefinitionRegistrar接口类型
 *   5）ImportSelector接口类型：返回需要导入的类名数组，可以是任何普通类、配置类(如@Configuration/@Bean/@ComponentScan等标注的类)等
 *   6）DeferredImportSelector接口类型：ImportSelector的子接口，延迟导入并可以指定导入类的处理顺序
 * </pre>
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(DbSnowFlakeWorkerRegistrar.class)
public @interface DbSnowflakeIdGenerator {

    String jdbcTemplateRef() default "";

    String bizTag() default "disjob";

    int sequenceBitLength() default 14;

    int workerIdBitLength() default 8;

    /**
     * Basic DbDistributedSnowflake
     */
    class BasicDbdSnowflake extends DbDistributedSnowflake {

        BasicDbdSnowflake(JdbcTemplate jdbcTemplate,
                          Object supervisor,
                          String bizTag,
                          int sequenceBitLength,
                          int workerIdBitLength) {
            super(jdbcTemplate, bizTag, serializeSupervisor(supervisor), sequenceBitLength, workerIdBitLength);
        }

        private static final String CURRENT_SUPERVISOR_CLASS_NAME = "cn.ponfee.disjob.core.base.Supervisor$Current$1";

        /**
         * Current supervisor spring bean name
         *
         * @see cn.ponfee.disjob.core.base.JobConstants#SPRING_BEAN_NAME_CURRENT_SUPERVISOR
         */
        protected static final String SPRING_BEAN_NAME_CURRENT_SUPERVISOR = "disjob.bean.current-supervisor";

        /**
         * String of Supervisor.Current serialization
         *
         * @param supervisor the current supervisor
         * @return serialization string
         * @see cn.ponfee.disjob.core.base.Supervisor.Current#serialize()
         */
        private static String serializeSupervisor(Object supervisor) {
            Class<?> clazz = supervisor.getClass();
            Assert.isTrue(CURRENT_SUPERVISOR_CLASS_NAME.equals(clazz.getName()), "Not Supervisor$Current$1 instance: " + clazz);
            return (String) ReflectionUtils.invokeMethod(ReflectionUtils.findMethod(clazz, "serialize"), supervisor);
        }
    }

    /**
     * Annotated DbDistributedSnowflake
     */
    class AnnotatedDbSnowflake extends BasicDbdSnowflake {

        AnnotatedDbSnowflake(@Autowired JdbcTemplate jdbcTemplate, // use @Primary JdbcTemplate bean
                             @Autowired @Qualifier(SPRING_BEAN_NAME_CURRENT_SUPERVISOR) Object supervisor,
                             String bizTag,
                             int sequenceBitLength,
                             int workerIdBitLength) {
            super(jdbcTemplate, supervisor, bizTag, sequenceBitLength, workerIdBitLength);
        }
    }

    class DbSnowFlakeWorkerRegistrar implements ImportBeanDefinitionRegistrar {

        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            DbSnowflakeIdGenerator config = SpringUtils.parseAnnotation(DbSnowflakeIdGenerator.class, importingClassMetadata);
            if (config == null) {
                return;
            }

            String jdbcTemplateRef = config.jdbcTemplateRef();
            GenericBeanDefinition bd;
            if (StringUtils.isBlank(jdbcTemplateRef)) {
                bd = new AnnotatedGenericBeanDefinition(AnnotatedDbSnowflake.class);
            } else {
                bd = new GenericBeanDefinition();
                bd.setBeanClass(BasicDbdSnowflake.class);
                bd.getConstructorArgumentValues().addIndexedArgumentValue(0, new RuntimeBeanReference(jdbcTemplateRef));
                bd.getConstructorArgumentValues().addIndexedArgumentValue(1, new RuntimeBeanReference(BasicDbdSnowflake.SPRING_BEAN_NAME_CURRENT_SUPERVISOR));
            }

            bd.getConstructorArgumentValues().addIndexedArgumentValue(2, config.bizTag());
            bd.getConstructorArgumentValues().addIndexedArgumentValue(3, config.sequenceBitLength());
            bd.getConstructorArgumentValues().addIndexedArgumentValue(4, config.workerIdBitLength());

            bd.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            bd.setScope(BeanDefinition.SCOPE_SINGLETON);
            registry.registerBeanDefinition(IdGenerator.class.getName(), bd);
        }
    }

}
