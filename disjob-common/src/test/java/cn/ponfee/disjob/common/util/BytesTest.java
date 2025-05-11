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

import com.google.common.primitives.Longs;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Bytes test
 *
 * @author Ponfee
 */
public class BytesTest {

    @Test
    public void testEncodeHex() {
        for (int i = 0; i < 1000; i++) {
            byte[] bytes1 = RandomUtils.nextBytes(ThreadLocalRandom.current().nextInt(100) + 1);
            String s1 = Bytes.encodeHex(bytes1);
            String s2 = Hex.encodeHexString(bytes1);
            Assertions.assertEquals(s1, s2);
            byte[] bytes2 = Bytes.decodeHex(s1);
            Assertions.assertArrayEquals(bytes1, bytes2);
        }
    }

    @Test
    public void testToHexString() {
        for (int i = 0; i < 1000; i++) {
            long value = ThreadLocalRandom.current().nextLong();
            byte[] bytes1 = Bytes.toBytes(value);
            byte[] bytes2 = Longs.toByteArray(value);
            Assertions.assertArrayEquals(bytes1, bytes2);
            Assertions.assertEquals(value, Bytes.toLong(bytes1));
        }
    }

    @Test
    public void testToString() {
        Assertions.assertNull(Bytes.toString(null, StandardCharsets.UTF_8));
        Assertions.assertEquals("", Bytes.toString(new byte[0], StandardCharsets.UTF_8));
        Assertions.assertEquals("abc", Bytes.toString("abc".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));

        Assertions.assertNull(Bytes.toBytes((String) null, StandardCharsets.UTF_8));
        Assertions.assertEquals(0, Bytes.toBytes("", StandardCharsets.UTF_8).length);
        Assertions.assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), Bytes.toBytes("abc", StandardCharsets.UTF_8));
    }

}
