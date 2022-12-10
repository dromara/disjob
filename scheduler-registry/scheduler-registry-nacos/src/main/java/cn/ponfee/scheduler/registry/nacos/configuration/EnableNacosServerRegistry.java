package cn.ponfee.scheduler.registry.nacos.configuration;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enable nacos server registry
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(NacosServerRegistryConfigure.class)
public @interface EnableNacosServerRegistry {

}
