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

package cn.ponfee.disjob.common.util;

import cn.ponfee.disjob.common.base.CombinedInvocationHandler;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Proxy utility class.
 *
 * @author Ponfee
 */
public final class ProxyUtils {

    /**
     * Creates proxy instance based jdk
     *
     * @param invocationHandler the jdk invocation handler
     * @param cls               the interface classes
     * @param <T>               the interface type
     * @return jdk proxy instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(java.lang.reflect.InvocationHandler invocationHandler, Class<?>... cls) {
        return (T) java.lang.reflect.Proxy.newProxyInstance(cls[0].getClassLoader(), cls, invocationHandler);
    }

    /**
     * Creates proxy instance based cglib
     * <p>需要默认的无参构造函数
     *
     * @param invocationHandler the spring cglib invocation handler
     * @param superCls          the super class
     * @param <T>               the super class type
     * @return cglib proxy instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(org.springframework.cglib.proxy.InvocationHandler invocationHandler, Class<?> superCls) {
        // org.springframework.cglib.proxy.Proxy的这种方式必须为interface
        // return (T) org.springframework.cglib.proxy.Proxy.newProxyInstance(superCls[0].getClassLoader(), superCls, invocationHandler);

        // org.springframework.cglib.proxy.Enhancer的这种方式同时支持class、interface
        return (T) org.springframework.cglib.proxy.Enhancer.create(superCls, invocationHandler);
    }

    public static <T> T create(CombinedInvocationHandler invocationHandler, Class<?> cls) {
        if (cls.isInterface()) {
            return create((java.lang.reflect.InvocationHandler) invocationHandler, cls);
        } else {
            return create((org.springframework.cglib.proxy.InvocationHandler) invocationHandler, cls);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Constructor<T> getProxyConstructor(Class<T> cls) {
        try {
            // org.springframework.cglib.proxy.Proxy的这种方式必须为interface
            // Class<T> proxyClass = org.springframework.cglib.proxy.Proxy.getProxyClass(cls.getClassLoader(), new Class[]{cls});
            // Constructor<T> proxyConstructor = proxyClass.getConstructor(org.springframework.cglib.proxy.InvocationHandler.class);

            Class<T> proxyClass = (Class<T>) java.lang.reflect.Proxy.getProxyClass(cls.getClassLoader(), cls);
            Constructor<T> proxyConstructor = proxyClass.getConstructor(java.lang.reflect.InvocationHandler.class);
            proxyConstructor.setAccessible(true);
            return proxyConstructor;
        } catch (Exception e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * Creates proxied annotation type object
     *
     * @param annotationType the annotation type
     * @param attributes     the attribute map
     * @param <A>            annotation type
     * @return proxied annotation type object
     */
    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A create(Class<A> annotationType, Map<String, Object> attributes) {
        return (A) java.lang.reflect.Proxy.newProxyInstance(
            annotationType.getClassLoader(),
            new Class<?>[]{annotationType},
            new AnnotationInvocationHandler(annotationType, attributes)
        );
    }

    /**
     * <pre>
     * 获取原始对象(被代理的对象)，如：`Service`中的方法加了`@Transactional`注解，`Controller`中被注入的`Service`实际是代理对象，
     * 可以使用`getTargetObject`获取原始的`Service`对象
     * </pre>
     *
     * @param object the proxy object
     * @return target object
     * @throws Exception if occur exception
     */
    public static Object getTargetObject(Object object) throws Exception {
        if (!AopUtils.isAopProxy(object)) {
            return object;
        }
        if (object instanceof Advised) {
            return ((Advised) object).getTargetSource().getTarget();
        }
        if (AopUtils.isJdkDynamicProxy(object)) {
            return getProxyTargetObject(FieldUtils.readField(object, "h", true));
        }
        if (AopUtils.isCglibProxy(object)) {
            return getProxyTargetObject(FieldUtils.readField(object, "CGLIB$CALLBACK_0", true));
        }
        return object;
    }

    // -----------------------------------------------------------------private methods or class

    private static Object getProxyTargetObject(Object proxy) throws Exception {
        AdvisedSupport advisedSupport = (AdvisedSupport) FieldUtils.readField(proxy, "advised", true);
        return advisedSupport.getTargetSource().getTarget();
    }

    private static class AnnotationInvocationHandler implements java.lang.reflect.InvocationHandler {
        private final Class<? extends Annotation> annotationType;
        private final Map<String, Object> attributes;

        private AnnotationInvocationHandler(Class<? extends Annotation> annotationType, Map<String, Object> attributes) {
            this.annotationType = annotationType;
            this.attributes = attributes;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String methodName = method.getName();
            int parameterCount = method.getParameterCount();
            if (parameterCount > 0) {
                // Annotation中只有`#equals(java.lang.Object)`方法才会带有参数
                Assert.isTrue(parameterCount == 1 && "equals".equals(methodName), () -> "Unknown annotation method: " + method);
                return proxy == args[0];
            }
            switch (methodName) {
                case "annotationType":
                    return annotationType;
                case "hashCode":
                    return hashCode(proxy);
                case "toString":
                    return annotationType.getName() + "@" + hashCode(proxy);
                // 以下方法在`java.lang.Object`类中用了final修饰，不会被覆写，实际调用不会走进来
                case "getClass":
                case "notify":
                case "notifyAll":
                case "wait":
                    throw new UnsupportedOperationException("Unexpected annotation method: " + method);
                default:
                    Object value = (attributes == null) ? null : attributes.get(methodName);
                    return value == null ? method.getDefaultValue() : ObjectUtils.cast(value, method.getReturnType());
            }
        }

        private int hashCode(Object proxy) {
            return System.identityHashCode(proxy);
        }
    }

}
