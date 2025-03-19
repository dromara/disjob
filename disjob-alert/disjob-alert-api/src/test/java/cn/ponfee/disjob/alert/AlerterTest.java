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

package cn.ponfee.disjob.alert;

import cn.ponfee.disjob.alert.enums.AlertType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Alerter test
 *
 * @author Ponfee
 */
class AlerterTest {

    @Test
    void test2power() {
        List<Integer> list = new ArrayList<>();
        for (int n = 0; n < 2000; n++) {
            if ((n & (n - 1)) == 0) {
                list.add(n);
            }
        }
        Assertions.assertThat(list.toString()).isEqualTo("[0, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024]");
    }

    @Test
    void testMatches() {
        Assertions.assertThat(AlertType.ALARM.matches(0)).isFalse();
        Assertions.assertThat(AlertType.ALARM.matches(1)).isTrue();
        Assertions.assertThat(AlertType.ALARM.matches(2)).isFalse();
        Assertions.assertThat(AlertType.ALARM.matches(3)).isTrue();

        Assertions.assertThat(AlertType.NOTICE.matches(0)).isFalse();
        Assertions.assertThat(AlertType.NOTICE.matches(1)).isFalse();
        Assertions.assertThat(AlertType.NOTICE.matches(2)).isTrue();
        Assertions.assertThat(AlertType.NOTICE.matches(3)).isTrue();
    }

    @Test
    void testCheck() {
        Assertions.assertThat(Arrays.stream(AlertType.values()).mapToInt(AlertType::value).reduce(0, (a, b) -> a ^ b)).isEqualTo(3);
        Assertions.assertThat(IntStream.of(1, 2, 4, 8).reduce(0, (a, b) -> a ^ b)).isEqualTo(15);

        AlertType.check(0);
        AlertType.check(1);
        AlertType.check(2);
        AlertType.check(3);

        Assertions.assertThatThrownBy(() -> AlertType.check(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid alert type val: -1");

        Assertions.assertThatThrownBy(() -> AlertType.check(4))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid alert type val: 4");

        Assertions.assertThatThrownBy(() -> AlertType.check(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid alert type val: null");
    }

}
