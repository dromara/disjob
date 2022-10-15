package cn.ponfee.scheduler.registry.zookeeper.configuration;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enable zookeeper server registry
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(ZookeeperServerRegistryConfigure.class)
public @interface EnableZookeeperServerRegistry {

}
