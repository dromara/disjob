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

package cn.ponfee.disjob.supervisor.util;

import cn.ponfee.disjob.common.spring.RpcController;
import cn.ponfee.disjob.common.util.ProxyUtils;
import cn.ponfee.disjob.core.base.WorkerRpcService;
import cn.ponfee.disjob.worker.provider.WorkerRpcProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;

/**
 * Proxy test
 *
 * @author Ponfee
 */
public class ProxyTest {

    @Test
    public void testProxy() {
        Object object = ProxyUtils.create((InvocationHandler) (proxy, method, args) -> null, WorkerRpcProvider.class);
        Assertions.assertTrue(Proxy.isProxyClass(object.getClass()));
        Assertions.assertEquals("[interface cn.ponfee.disjob.worker.provider.WorkerRpcProvider]", Arrays.toString(object.getClass().getInterfaces()));
        Assertions.assertTrue(Object.class.isAssignableFrom(Object.class));
        Assertions.assertTrue(String.class.isAssignableFrom(String.class));
        Assertions.assertTrue(Object.class.isAssignableFrom(String.class));
        Assertions.assertFalse(String.class.isAssignableFrom(Object.class));
        Assertions.assertEquals(0, Object.class.getInterfaces().length);
        Assertions.assertNull(Object.class.getSuperclass());
        Assertions.assertThrows(NullPointerException.class, () -> Object.class.isAssignableFrom(null));
    }

    @Test
    public void testAnnotation() {
        Assertions.assertNull(AnnotationUtils.findAnnotation(WorkerRpcService.class, RpcController.class));
        Assertions.assertNull(AnnotationUtils.findAnnotation(WorkerRpcProvider.WorkerRpcLocal.class, RpcController.class));

        Assertions.assertNotNull(AnnotationUtils.findAnnotation(WorkerRpcProvider.class, RpcController.class));
        Assertions.assertNotNull(AnnotationUtils.findAnnotation(A.class, RpcController.class));

        Assertions.assertTrue(WorkerRpcProvider.class.isAnnotationPresent(RpcController.class));
    }

    private static abstract class A implements WorkerRpcProvider {

    }

}
