/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.support.AopUtils;
import org.springframework.cglib.proxy.Enhancer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * Proxy utility class.
 *
 * @author Ponfee
 */
public class ProxyUtils {

    /**
     * Creates jdk proxy instance
     *
     * @param invocationHandler jdk invocation handler
     * @param interfaceTypes    the interface class array
     * @param <T>               the interface type
     * @return jdk proxy instance
     */
    public static <T> T create(InvocationHandler invocationHandler, Class<?>... interfaceTypes) {
        return (T) Proxy.newProxyInstance(interfaceTypes[0].getClassLoader(), interfaceTypes, invocationHandler);
    }

    /**
     * Returns the proxy target object
     *
     * @param object the object
     * @return target object
     * @throws Exception
     */
    public static Object getTargetObject(Object object) throws Exception {
        if (!AopUtils.isAopProxy(object)) {
            return object;
        }
        if (object instanceof Advised) {
            return ((Advised) object).getTargetSource().getTarget();
        }
        if (AopUtils.isJdkDynamicProxy(object)) {
            return getProxyTargetObject(Fields.get(object, "h"));
        }
        if (AopUtils.isCglibProxy(object)) {
            return getProxyTargetObject(Fields.get(object, "CGLIB$CALLBACK_0"));
        }
        return object;
    }

    public static <T> T createBrokenProxy(Class<T> type, Class<?>[] argumentTypes, Object[] arguments) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(type);
        enhancer.setUseCache(true);
        enhancer.setInterceptDuringConstruction(false);
        enhancer.setCallback((org.springframework.cglib.proxy.InvocationHandler) (proxy, method, args) -> {
            throw new UnsupportedOperationException("Broken proxy cannot execute method: " + method.toGenericString());
        });
        return (T) enhancer.create(argumentTypes, arguments);
    }

    private static Object getProxyTargetObject(Object proxy) throws Exception {
        AdvisedSupport advisedSupport = (AdvisedSupport) Fields.get(proxy, "advised");
        return advisedSupport.getTargetSource().getTarget();
    }

}
