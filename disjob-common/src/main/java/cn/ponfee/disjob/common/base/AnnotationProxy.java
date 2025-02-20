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

package cn.ponfee.disjob.common.base;

import cn.ponfee.disjob.common.util.ObjectUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * Annotation proxy
 *
 * @author Ponfee
 */
public class AnnotationProxy {

    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A create(Class<A> annotationType, Map<String, Object> attributes) {
        return (A) Proxy.newProxyInstance(
            annotationType.getClassLoader(),
            new Class[]{annotationType},
            new AnnotationInvocationHandler(attributes)
        );
    }

    private static class AnnotationInvocationHandler implements InvocationHandler {
        private final Map<String, Object> attributes;

        private AnnotationInvocationHandler(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            return (attributes != null && attributes.containsKey(methodName))
                ? ObjectUtils.cast(attributes.get(methodName), method.getReturnType())
                : method.getDefaultValue();
        }
    }

}
