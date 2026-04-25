package com.ruoyi.common.annotation;

import java.lang.annotation.*;

/**
 * 数据权限过滤注解
 *
 * @author ruoyi
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope
{
    /**
     * 用户表的别名
     */
    String userAlias() default "";

    /**
     * 部门表的别名
     */
    String deptAlias() default "";

    /**
     * 用户字段名
     */
    String userField() default "user_id";

    /**
     * 部门字段名
     */
    String deptField() default "dept_id";

    /**
     * 权限字符（用于多个角色匹配符合要求的权限）默认根据权限注解@RequiresPermissions获取，多个权限用逗号分隔开来
     */
    String permission() default "";
}
