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

package cn.ponfee.disjob.supervisor.util;

import cn.ponfee.disjob.common.tuple.Tuple2;
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
