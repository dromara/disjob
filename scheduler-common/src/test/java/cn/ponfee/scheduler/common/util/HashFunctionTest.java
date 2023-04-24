/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.util;

import cn.ponfee.scheduler.common.base.ConsistentHash;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * HashFunctionTest
 *
 * @author Ponfee
 */
public class HashFunctionTest {

    @Test
    public void testMd5() {
        Assertions.assertNotEquals(
            ConsistentHash.HashFunction.MD5.hash("test1"),
            ConsistentHash.HashFunction.MD5.hash("test2")
        );
    }
}
