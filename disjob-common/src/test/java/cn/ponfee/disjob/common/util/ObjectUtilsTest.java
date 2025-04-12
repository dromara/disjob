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

import cn.ponfee.disjob.common.dag.DAGNode;
import cn.ponfee.disjob.common.spring.ResourceScanner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * ClassUtils test
 *
 * @author Ponfee
 */
public class ObjectUtilsTest {

    @Test
    public void testNewInstance() {
        Assertions.assertNotNull(NetUtils.getLocalHost());
        Assertions.assertTrue(Boolean.parseBoolean("True"));
        Assertions.assertFalse(Boolean.parseBoolean("1"));
        Assertions.assertFalse(Boolean.parseBoolean("0"));
    }

    @Test
    public void testGetValue() {
        Assertions.assertEquals(ObjectUtils.getValue(DAGNode.START, "topology"), 0);
        Assertions.assertEquals(ObjectUtils.getValue(DAGNode.START, "name"), "Start");
        Assertions.assertNotEquals(System.identityHashCode(new String("abc")), System.identityHashCode(new String("abc")));

        long max = Long.MAX_VALUE;
        Long max1 = max;
        Long max2 = max;
        Long max3 = Long.MAX_VALUE;
        Assertions.assertTrue(max1 != max2);
        Assertions.assertTrue(max1 != max3);
        Assertions.assertTrue(max2 != max3);
    }

    @Test
    @Disabled
    public void testStaticFinalMethod() {
        findStaticFinalMethod("cn/ponfee/disjob/common/**/*.class");
        findStaticFinalMethod("cn/ponfee/disjob/core/**/*.class");
        findStaticFinalMethod("cn/ponfee/disjob/registry/**/*.class");
        findStaticFinalMethod("cn/ponfee/disjob/dispatch/**/*.class");
        findStaticFinalMethod("cn/ponfee/disjob/worker/**/*.class");
    }

    private static void findStaticFinalMethod(String packageName) {
        for (Class<?> clazz : new ResourceScanner(packageName).scan4class()) {
            Set<Method> methods = new HashSet<>();
            methods.addAll(Arrays.asList(clazz.getDeclaredMethods()));
            methods.addAll(Arrays.asList(clazz.getMethods()));
            for (Method method : methods) {
                if (Modifier.isStatic(method.getModifiers()) && Modifier.isFinal(method.getModifiers())) {
                    String cname = method.getDeclaringClass().getName();
                    if (!cname.contains("$") && cname.startsWith("cn.ponfee")) {
                        System.err.println(clazz + "  " + method);
                    }
                }
            }
        }
    }

}
