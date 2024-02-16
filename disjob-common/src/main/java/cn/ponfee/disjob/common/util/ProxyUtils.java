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

import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * Proxy utility class.
 *
 * @author Ponfee
 */
public final class ProxyUtils {

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

    private static Object getProxyTargetObject(Object proxy) throws Exception {
        AdvisedSupport advisedSupport = (AdvisedSupport) Fields.get(proxy, "advised");
        return advisedSupport.getTargetSource().getTarget();
    }

}
