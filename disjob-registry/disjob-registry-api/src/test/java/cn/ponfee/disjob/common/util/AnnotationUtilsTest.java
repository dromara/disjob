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

import cn.ponfee.disjob.common.spring.RpcController;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AnnotationUtils test
 *
 * @author Ponfee
 */
public class AnnotationUtilsTest {

    @RequestMapping(value = "parent/controller")
    static class ParentController {
    }

    static class ChildController extends ParentController {
    }

    @RpcController("super")
    static class SupClass {
    }

    static class SubClass extends SupClass {
    }

    static class HelloController {
        @GetMapping("/hello")
        public String hello(String name) {
            return "Hello " + name + "!";
        }
    }

    @Test
    public void test1() {
        assertNotNull(AnnotationUtils.getAnnotation(ParentController.class, RequestMapping.class));
        assertNull(AnnotationUtils.getAnnotation(ChildController.class, RequestMapping.class));

        assertNotNull(AnnotationUtils.findAnnotation(ParentController.class, RequestMapping.class));
        assertNotNull(AnnotationUtils.findAnnotation(ChildController.class, RequestMapping.class));

        assertTrue(AnnotatedElementUtils.isAnnotated(ParentController.class, RequestMapping.class));
        assertNotNull(AnnotatedElementUtils.getMergedAnnotation(ParentController.class, RequestMapping.class));
        assertFalse(AnnotatedElementUtils.isAnnotated(ChildController.class, RequestMapping.class));
        assertNull(AnnotatedElementUtils.getMergedAnnotation(ChildController.class, RequestMapping.class));

        assertTrue(AnnotatedElementUtils.hasAnnotation(ParentController.class, RequestMapping.class));
        assertNotNull(AnnotatedElementUtils.findMergedAnnotation(ParentController.class, RequestMapping.class));
        assertTrue(AnnotatedElementUtils.hasAnnotation(ChildController.class, RequestMapping.class));
        assertNotNull(AnnotatedElementUtils.findMergedAnnotation(ChildController.class, RequestMapping.class));
    }

    @Test
    public void test2() throws NoSuchMethodException {
        // 不支持`@AliasFor`语义
        assertEquals("", AnnotationUtils.findAnnotation(SubClass.class, Component.class).value());
        assertEquals("super", AnnotatedElementUtils.findMergedAnnotation(SubClass.class, Component.class).value());

        Method method = HelloController.class.getMethod("hello", String.class);
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        assertEquals(1, mapping.value().length);
        assertEquals("/hello", mapping.value()[0]);
    }

}


