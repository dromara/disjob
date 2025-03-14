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

import cn.ponfee.disjob.common.base.Symbol;
import cn.ponfee.disjob.common.spring.RpcController;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.common.util.ProxyUtils;
import cn.ponfee.disjob.common.util.UuidUtils;
import cn.ponfee.disjob.core.base.WorkerRpcService;
import cn.ponfee.disjob.worker.provider.WorkerRpcProvider;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.annotation.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;

/**
 * Proxy utils test
 *
 * @author Ponfee
 */
class ProxyUtilsTest {

    @Test
    void testCreate() {
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
    void testAnnotation() {
        Assertions.assertNull(AnnotationUtils.findAnnotation(WorkerRpcService.class, RpcController.class));
        Assertions.assertNull(AnnotationUtils.findAnnotation(WorkerRpcProvider.WorkerRpcLocal.class, RpcController.class));

        Assertions.assertNotNull(AnnotationUtils.findAnnotation(WorkerRpcProvider.class, RpcController.class));
        Assertions.assertNotNull(AnnotationUtils.findAnnotation(A.class, RpcController.class));

        Assertions.assertTrue(WorkerRpcProvider.class.isAnnotationPresent(RpcController.class));
    }

    @Test
    void testCreateAnnotation1() {
        Map<String, Object> map = ImmutableMap.of(
            "basePackageClasses", Symbol.class,
            "mapperLocations", new String[]{"a", "b"},
            "basePackages", "x",
            "defaultFetchSize", 99,
            "equals", true
        );
        Ann ann = ProxyUtils.create(Ann.class, map);
        org.assertj.core.api.Assertions.assertThat(Jsons.toJson(ann.basePackageClasses())).isEqualTo("[\"cn.ponfee.disjob.common.base.Symbol\"]");
        org.assertj.core.api.Assertions.assertThat(Jsons.toJson(ann.mapperLocations())).isEqualTo("[\"a\",\"b\"]");
        org.assertj.core.api.Assertions.assertThat(Jsons.toJson(ann.basePackages())).isEqualTo("[\"x\"]");
        org.assertj.core.api.Assertions.assertThat(Jsons.toJson(ann.defaultFetchSize())).isEqualTo("99");
        org.assertj.core.api.Assertions.assertThat(Jsons.toJson(ann.defaultStatementTimeout())).isEqualTo("25");
        org.assertj.core.api.Assertions.assertThat(ann.equals()).isTrue();
    }

    @Test
    void testCreateAnnotation2() {
        Map<String, Object> map = ImmutableMap.of(
            "basePackageClasses", new Class[]{Symbol.class, UuidUtils.class},
            "mapperLocations", new String[]{"a", "b"},
            "basePackages", "x"
        );
        Ann ann = ProxyUtils.create(Ann.class, map);
        org.assertj.core.api.Assertions.assertThat(Jsons.toJson(ann.basePackageClasses())).isEqualTo("[\"cn.ponfee.disjob.common.base.Symbol\",\"cn.ponfee.disjob.common.util.UuidUtils\"]");
        org.assertj.core.api.Assertions.assertThat(Jsons.toJson(ann.mapperLocations())).isEqualTo("[\"a\",\"b\"]");
        org.assertj.core.api.Assertions.assertThat(Jsons.toJson(ann.basePackages())).isEqualTo("[\"x\"]");

        org.assertj.core.api.Assertions.assertThat(Object[].class.isAssignableFrom(Object[].class)).isTrue();
        org.assertj.core.api.Assertions.assertThat(String[].class.isAssignableFrom(String[].class)).isTrue();
        org.assertj.core.api.Assertions.assertThat(Object[].class.isAssignableFrom(String[].class)).isTrue();
        org.assertj.core.api.Assertions.assertThat(String[].class.isAssignableFrom(Object[].class)).isFalse();

        Object[] a = {new Object(), new Object()};
        String[] b = {"a", "b"};
        org.assertj.core.api.Assertions.assertThat(b instanceof Object[]).isTrue();
        org.assertj.core.api.Assertions.assertThat(b instanceof String[]).isTrue();
        org.assertj.core.api.Assertions.assertThat(a instanceof String[]).isFalse();
        Assertions.assertEquals(b instanceof Object[], Object[].class.isInstance(b));
        Assertions.assertEquals(b instanceof String[], String[].class.isInstance(b));
        Assertions.assertEquals(a instanceof String[], String[].class.isInstance(a));

        a = b;
        org.assertj.core.api.Assertions.assertThat(Arrays.toString(a)).isEqualTo("[a, b]");
    }

    @Test
    void testCreateAnnotation3() throws InterruptedException {
        Ann ann = ProxyUtils.create(Ann.class, null);
        org.assertj.core.api.Assertions.assertThat(ann.getClass() == Ann.class).isFalse();
        org.assertj.core.api.Assertions.assertThat(Ann.class).isAssignableFrom(ann.getClass());
        org.assertj.core.api.Assertions.assertThat(ann.annotationType() == Ann.class).isTrue();

        org.assertj.core.api.Assertions.assertThat(ann.equals(ann)).isTrue();
        org.assertj.core.api.Assertions.assertThat(ann.equals(null)).isFalse();

        org.assertj.core.api.Assertions.assertThat(ann.toString()).isEqualTo("cn.ponfee.disjob.supervisor.util.ProxyUtilsTest$Ann@" + ann.hashCode());

        org.assertj.core.api.Assertions.assertThat(ann.equals()).isFalse();

        synchronized (ann) {
            ann.notify();
        }
        synchronized (ann) {
            ann.notifyAll();
        }
        /*
        synchronized (ann) {
            ann.wait();
        }
        */
        synchronized (ann) {
            ann.wait(1);
        }
        synchronized (ann) {
            ann.wait(1, 1);
        }
    }

    // ------------------------------------------------------------

    private static abstract class A implements WorkerRpcProvider {

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Documented
    private @interface Ann {
        boolean equals() default false;

        String dataSourceName() default "";

        String[] mapperLocations() default {};

        String[] basePackages() default {};

        Class<?>[] basePackageClasses() default {};

        boolean mapUnderscoreToCamelCase() default true;

        String typeAliasesPackage() default "";

        int defaultFetchSize() default 100;

        int defaultStatementTimeout() default 25;

        boolean primary() default false;
    }

}
