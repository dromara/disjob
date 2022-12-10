package cn.ponfee.scheduler.registry.etcd.configuration;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enable etcd server registry
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(EtcdServerRegistryConfigure.class)
public @interface EnableEtcdServerRegistry {

}
