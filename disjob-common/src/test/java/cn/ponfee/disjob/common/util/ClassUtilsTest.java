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

import cn.ponfee.disjob.common.base.Symbol;
import org.apache.commons.io.HexDump;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ClassUtils test
 *
 * @author Ponfee
 */
public class ClassUtilsTest {

    @Test
    public void testNewInstance() {
        Assertions.assertFalse(ClassUtils.newInstance(boolean.class));
        Assertions.assertFalse(ClassUtils.newInstance(Boolean.class));

        Assertions.assertEquals(Symbol.Char.ZERO, ClassUtils.newInstance(char.class));
        Assertions.assertEquals(Symbol.Char.ZERO, ClassUtils.newInstance(Character.class));

        Assertions.assertEquals((byte) 0, ClassUtils.newInstance(byte.class));
        Assertions.assertEquals((byte) 0, ClassUtils.newInstance(Byte.class));

        Assertions.assertEquals((short) 0, ClassUtils.newInstance(short.class));
        Assertions.assertEquals((short) 0, ClassUtils.newInstance(Short.class));

        Assertions.assertEquals(0, ClassUtils.newInstance(int.class));
        Assertions.assertEquals(0, ClassUtils.newInstance(Integer.class));

        Assertions.assertEquals(0L, ClassUtils.newInstance(long.class));
        Assertions.assertEquals(0L, ClassUtils.newInstance(Long.class));

        Assertions.assertEquals(new Float("0"), ClassUtils.newInstance(float.class));
        Assertions.assertEquals(new Float("0"), ClassUtils.newInstance(Float.class));

        Assertions.assertEquals(new Double("0"), ClassUtils.newInstance(double.class));
        Assertions.assertEquals(new Double("0"), ClassUtils.newInstance(Double.class));
    }

    @Test
    public void testGetClass() {
        Assertions.assertTrue(int.class.getClass() == Class.class);
        Assertions.assertTrue(int.class.getClass() == int.class.getClass().getClass());
        Assertions.assertTrue(int.class.getClass() == Object.class.getClass());
        Assertions.assertTrue(int.class.getClass() == Object.class.getClass().getClass());
        Assertions.assertTrue(Objects.equals(null, null));
    }

    @Test
    public void testAtomic() {
        AtomicReference<Class<?>> atomicReference = new AtomicReference<>();
        Assertions.assertTrue(atomicReference.compareAndSet(null, null));
        atomicReference.set(ClassUtilsTest.class);
        Assertions.assertTrue(atomicReference.compareAndSet(ClassUtilsTest.class, ClassUtilsTest.class));
        Assertions.assertFalse(atomicReference.compareAndSet(null, ClassUtilsTest.class));
        Assertions.assertEquals(PrimitiveTypes.BYTE.size(), 8);
    }

    @Test
    public void testClass() throws IOException {
        byte[] mainClassFileAsBytes = MavenProjects.getMainClassFileAsBytes(MavenProjects.class);
        HexDump.dump(mainClassFileAsBytes, System.out);
        System.out.print("\n\n---------------------------------------------------\n\n");

        byte[] testClassFileAsBytes = MavenProjects.getTestClassFileAsBytes(ClassUtilsTest.class);
        HexDump.dump(testClassFileAsBytes, System.out);
    }

}
