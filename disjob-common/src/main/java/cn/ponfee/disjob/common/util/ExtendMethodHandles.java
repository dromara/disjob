/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * <pre>
 * jdk8中如果直接调用{@link MethodHandles#lookup()}获取到的{@link MethodHandles.Lookup}
 * 在调用方法 {@link MethodHandles.Lookup#findSpecial(java.lang.Class, java.lang.String, java.lang.invoke.MethodType, java.lang.Class)}
 * 和{@link MethodHandles.Lookup#unreflectSpecial(java.lang.reflect.Method, java.lang.Class)}
 * 获取父类方法句柄{@link MethodHandle}时
 * 可能出现权限不够, 抛出如下异常, 所以通过反射创建{@link MethodHandles.Lookup}解决该问题.
 *
 *  java.lang.IllegalAccessException: no private access for invokespecial: interface com.example.demo.methodhandle.UserService, from com.example.demo.methodhandle.UserServiceInvoke
 *  at java.lang.invoke.MemberName.makeAccessException(MemberName.java:850)
 *  at java.lang.invoke.MethodHandles$Lookup.checkSpecialCaller(MethodHandles.java:1572)
 *
 * 而jdk11中直接调用{@link MethodHandles#lookup()}获取到的{@link MethodHandles.Lookup},也只能对接口类型才会权限获取方法的方法句柄{@link MethodHandle}.
 * 如果是普通类型Class,需要使用jdk9开始提供的 MethodHandles#privateLookupIn(java.lang.Class, java.lang.invoke.MethodHandles.Lookup)方法.
 * </pre>
 *
 * <a href="https://blog.csdn.net/u013202238/article/details/108687086">参考文章</a>
 *
 * @author Ponfee
 */
public class ExtendMethodHandles {


    public static final Function<Class<?>, Lookup> METHOD_LOOKUP;

    static {
        // 先查询jdk9开始提供的“java.lang.invoke.MethodHandles.privateLookupIn”方法
        Method java9PrivateLookupInMethod = ClassUtils.getMethod(MethodHandles.class, "privateLookupIn", Class.class, Lookup.class);
        if (java9PrivateLookupInMethod != null) {
            METHOD_LOOKUP = callerClass -> {
                try {
                    return (Lookup) java9PrivateLookupInMethod.invoke(MethodHandles.class, callerClass, MethodHandles.lookup());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            };
        } else {
            // 查询jdk8版本：这种方式也适用于jdk9及以上的版本，但优先上面的可以避免jdk9反射警告
            Constructor<Lookup> java8LookupConstructor = ClassUtils.getConstructor(Lookup.class, Class.class, int.class);
            if (java8LookupConstructor == null) {
                // 未找到则可能是jdk8以下版本
                throw new IllegalStateException("Not found 'privateLookupIn(Class, Lookup)' and 'Lookup(Class, int)' method in java.lang.invoke.MethodHandles.");
            }
            java8LookupConstructor.setAccessible(true);
            METHOD_LOOKUP = new Function<Class<?>, Lookup>() {
                private static final int ALLOWED_MODES = Lookup.PRIVATE | Lookup.PROTECTED | Lookup.PACKAGE | Lookup.PUBLIC;

                @Override
                public Lookup apply(Class<?> callerClass) {
                    try {
                        return java8LookupConstructor.newInstance(callerClass, ALLOWED_MODES);
                    } catch (Exception e) {
                        throw new IllegalStateException("No found 'Lookup(Class, int)' method in java.lang.invoke.MethodHandles.", e);
                    }
                }
            };
        }
    }

    /**
     * java9中的MethodHandles.lookup()方法返回的Lookup对象
     * 有权限访问specialCaller != lookupClass()的类
     * 但是只能适用于接口, {@link java.lang.invoke.MethodHandles.Lookup#checkSpecialCaller(Class)}}
     */
    public static MethodHandle getSpecialMethodHandle(Method parentMethod) {
        Class<?> declaringClass = parentMethod.getDeclaringClass();
        Lookup lookup = METHOD_LOOKUP.apply(declaringClass);
        try {
            return lookup.in(declaringClass).unreflectSpecial(parentMethod, declaringClass);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
