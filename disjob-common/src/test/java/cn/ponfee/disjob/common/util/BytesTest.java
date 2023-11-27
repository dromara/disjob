/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import com.google.common.primitives.Longs;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

}
