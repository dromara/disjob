package cn.ponfee.scheduler.springboot.configure;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enable http task dispatch
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(HttpTaskDispatchingConfiguration.class)
public @interface EnableHttpTaskDispatcher {

}
