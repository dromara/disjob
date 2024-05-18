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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Objects;

/**
 * ClassUtils test
 *
 * @author Ponfee
 */
public class ClassUtilsTest {

    @Test
    public void testGetClass() {
        Assertions.assertTrue(int.class.getClass() == Class.class);
        Assertions.assertTrue(int.class.getClass() == int.class.getClass().getClass());
        Assertions.assertTrue(int.class.getClass() == Object.class.getClass());
        Assertions.assertTrue(int.class.getClass() == Object.class.getClass().getClass());
        Assertions.assertTrue(Objects.equals(null, null));
    }

}
