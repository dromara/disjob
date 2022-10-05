package cn.ponfee.scheduler.registry.consul.configuration;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enable consul server registry
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(ConsulServerRegistryConfigure.class)
public @interface EnableConsulServerRegistry {

}
