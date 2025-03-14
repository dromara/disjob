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

import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.support.AopUtils;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
     * @param interfaces        the interface class array
     * @param <T>               the interface type
     * @return jdk proxy instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(java.lang.reflect.InvocationHandler invocationHandler, Class<?>... interfaces) {
        return (T) Proxy.newProxyInstance(interfaces[0].getClassLoader(), interfaces, invocationHandler);
    }

    /**
     * Creates proxy instance based cglib
     * <p>需要默认的无参构造函数
     *
     * @param invocationHandler the spring cglib invocation handler
     * @param superClass        the super class
     * @param <T>               the super class type
     * @return cglib proxy instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(org.springframework.cglib.proxy.InvocationHandler invocationHandler, Class<?> superClass) {
        return (T) Enhancer.create(superClass, invocationHandler);
    }

    public static <T, H extends java.lang.reflect.InvocationHandler & org.springframework.cglib.proxy.InvocationHandler> T create(H invocationHandler, Class<?> cls) {
        if (cls.isInterface()) {
            return create((java.lang.reflect.InvocationHandler) invocationHandler, cls);
        } else {
            return create((org.springframework.cglib.proxy.InvocationHandler) invocationHandler, cls);
        }
    }

    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A create(Class<A> annotationType, Map<String, Object> attributes) {
        return (A) Proxy.newProxyInstance(
            annotationType.getClassLoader(),
            new Class<?>[]{annotationType},
            new AnnotationInvocationHandler(annotationType, attributes)
        );
    }

    /**
     * Returns the proxy target object
     *
     * @param object the object
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
                    return System.identityHashCode(proxy);
                case "toString":
                    return annotationType.getName() + "@" + proxy.hashCode();
                // 以下方法在`java.lang.Object`类中用了final修饰，不会被覆写，实际调用不会走进来
                case "getClass":
                case "notify":
                case "notifyAll":
                case "wait":
                    throw new RuntimeException("Unexpected annotation method: " + method);
                default:
                    Object value = attributes == null ? null : attributes.get(methodName);
                    return value == null ? method.getDefaultValue() : ObjectUtils.cast(value, method.getReturnType());
            }
        }
    }

}
