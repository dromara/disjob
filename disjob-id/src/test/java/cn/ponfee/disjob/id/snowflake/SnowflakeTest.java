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
        new Snowflake(workerIdBits, sequenceBits, 0);
        new Snowflake(workerIdBits, sequenceBits, 255);
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Snowflake(workerIdBits, sequenceBits, 256));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Snowflake(workerIdBits, sequenceBits, -1));
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
        Snowflake snowflake = new Snowflake(workerIdBits, sequenceBits, 16);
        for (int i = 0; i < 10; i++) {
            long num = snowflake.generateId();
            System.out.println(num + ": " + Bytes.toBinary(Bytes.toBytes(num)));
            System.out.println();
        }
    }
}
