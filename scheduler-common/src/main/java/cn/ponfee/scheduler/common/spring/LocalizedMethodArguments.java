package cn.ponfee.scheduler.common.spring;

import java.lang.annotation.*;

/**
 * Localization method arguments annotation definition.
 *
 * @author Ponfee
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface LocalizedMethodArguments {
}
