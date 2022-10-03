package cn.ponfee.scheduler.springboot.configure;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enable redis server registry
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(RedisServerRegistryConfigure.class)
public @interface EnableRedisServerRegistry {

}
