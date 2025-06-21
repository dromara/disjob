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

/**
 * Predicates Test
 *
 * @author Ponfee
 */
public class PredicatesTest {

    @Test
    public void test() {
        Assertions.assertEquals(1, Predicates.Y.value());
        Assertions.assertEquals('Y', Predicates.Y.code());
        Assertions.assertTrue(Predicates.Y.state());

        Assertions.assertEquals(0, Predicates.N.value());
        Assertions.assertEquals('N', Predicates.N.code());
        Assertions.assertFalse(Predicates.N.state());

        Assertions.assertTrue(Predicates.Y.equals(1));
        Assertions.assertTrue(Predicates.Y.equals("Y"));
        Assertions.assertTrue(Predicates.Y.equals('Y'));
        Assertions.assertTrue(Predicates.Y.equals(true));
        Assertions.assertFalse(Predicates.Y.equals(0));
        Assertions.assertFalse(Predicates.Y.equals("N"));
        Assertions.assertFalse(Predicates.Y.equals('N'));
        Assertions.assertFalse(Predicates.Y.equals(false));

        Assertions.assertFalse(Predicates.N.equals(1));
        Assertions.assertFalse(Predicates.N.equals("Y"));
        Assertions.assertFalse(Predicates.N.equals('Y'));
        Assertions.assertFalse(Predicates.N.equals(true));
        Assertions.assertTrue(Predicates.N.equals(0));
        Assertions.assertTrue(Predicates.N.equals("N"));
        Assertions.assertTrue(Predicates.N.equals('N'));
        Assertions.assertTrue(Predicates.N.equals(false));

        Assertions.assertTrue(Predicates.yes(1));
        Assertions.assertTrue(Predicates.yes("Y"));
        Assertions.assertTrue(Predicates.yes('Y'));
        Assertions.assertTrue(Predicates.yes(true));
        Assertions.assertFalse(Predicates.yes(0));
        Assertions.assertFalse(Predicates.yes("N"));
        Assertions.assertFalse(Predicates.yes('N'));
        Assertions.assertFalse(Predicates.yes(false));

        Assertions.assertFalse(Predicates.no(1));
        Assertions.assertFalse(Predicates.no("Y"));
        Assertions.assertFalse(Predicates.no('Y'));
        Assertions.assertFalse(Predicates.no(true));
        Assertions.assertTrue(Predicates.no(0));
        Assertions.assertTrue(Predicates.no("N"));
        Assertions.assertTrue(Predicates.no('N'));
        Assertions.assertTrue(Predicates.no(false));

        Assertions.assertEquals(Predicates.Y, Predicates.of(1));
        Assertions.assertEquals(Predicates.Y, Predicates.of("Y"));
        Assertions.assertEquals(Predicates.Y, Predicates.of('Y'));
        Assertions.assertEquals(Predicates.Y, Predicates.of(true));
        Assertions.assertEquals(Predicates.N, Predicates.of(0));
        Assertions.assertEquals(Predicates.N, Predicates.of("N"));
        Assertions.assertEquals(Predicates.N, Predicates.of('N'));
        Assertions.assertEquals(Predicates.N, Predicates.of(false));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Predicates.N.equals(2));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Predicates.N.equals("X"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Predicates.N.equals('X'));
    }

}
