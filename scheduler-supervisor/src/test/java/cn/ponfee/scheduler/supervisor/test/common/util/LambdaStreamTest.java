/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.test.common.util;

import cn.ponfee.scheduler.common.tuple.Tuple2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author Ponfee
 */
public class LambdaStreamTest {


    @Test
    public void testToMapHasNull() {
        Assertions.assertThrows(
            NullPointerException.class,
            () -> Arrays.stream(new Tuple2[]{Tuple2.of("a", "123"), Tuple2.of("b", null)}).collect(Collectors.toMap(t -> t.a, t -> t.b, (v1, v2) -> v2))
        );
    }

    @Test
    public void testToMapNoneNull() {
        Arrays.stream(new Tuple2[]{Tuple2.of("a", "123"), Tuple2.of("b", "abc")})
            .collect(Collectors.toMap(t -> t.a, t -> t.b, (v1, v2) -> v2));
    }
}
