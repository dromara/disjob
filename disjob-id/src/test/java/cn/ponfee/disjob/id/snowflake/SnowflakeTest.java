/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.id.snowflake;

import cn.ponfee.disjob.common.util.Bytes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * SnowflakeTest
 *
 * @author Ponfee
 */
public class SnowflakeTest {

    private static final int sequenceBits = 14;
    private static final int workerIdBits = 8;

    @Test
    public void testNew() {
        new Snowflake(0, sequenceBits, workerIdBits);
        new Snowflake(255, sequenceBits, workerIdBits);
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Snowflake(256, sequenceBits, workerIdBits));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Snowflake(-1, sequenceBits, workerIdBits));
    }

    @Test
    public void testShift() {
        Assertions.assertEquals(24 * 60 * 60 * 1000, TimeUnit.DAYS.toMillis(1));
        Assertions.assertEquals(256, 1 << 8);
        Assertions.assertEquals(1024, 1 << 10);

        int workerIdMaxCount = 1 << 8;
        Assertions.assertEquals(0, IntStream.range(0, workerIdMaxCount).min().getAsInt());
        Assertions.assertEquals(255, IntStream.range(0, workerIdMaxCount).max().getAsInt());
        Assertions.assertEquals(256, IntStream.range(0, workerIdMaxCount).count());
    }

    @Test
    public void testGenerateId() {
        Snowflake snowflake = new Snowflake(16, sequenceBits, workerIdBits);
        for (int i = 0; i < 10; i++) {
            long num = snowflake.generateId();
            System.out.println(num + ": " + Bytes.toBinary(Bytes.toBytes(num)));
            System.out.println();
        }
    }
}
