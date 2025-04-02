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

package cn.ponfee.disjob.core.alert;

import com.google.common.math.LongMath;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThat(list.toString()).isEqualTo("[0, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024]");
    }

    @Test
    void testMatches() {
        assertThat(AlertType.ALARM.matches(0)).isFalse();
        assertThat(AlertType.ALARM.matches(1)).isTrue();
        assertThat(AlertType.ALARM.matches(2)).isFalse();
        assertThat(AlertType.ALARM.matches(3)).isTrue();

        assertThat(AlertType.NOTICE.matches(0)).isFalse();
        assertThat(AlertType.NOTICE.matches(1)).isFalse();
        assertThat(AlertType.NOTICE.matches(2)).isTrue();
        assertThat(AlertType.NOTICE.matches(3)).isTrue();
    }

    @Test
    void testCheck1() {
        assertThat(Arrays.stream(AlertType.values()).mapToInt(AlertType::value).reduce(0, (a, b) -> a ^ b)).isEqualTo(3);
        assertThat(IntStream.of(1, 2, 4, 8).reduce(0, (a, b) -> a ^ b)).isEqualTo(15);
        assertThat(IntStream.of(1).reduce(0, (a, b) -> a ^ b)).isOne();
        assertThat(IntStream.of(0).reduce(0, (a, b) -> a ^ b)).isZero();
        assertThat(IntStream.of().reduce(0, (a, b) -> a ^ b)).isZero();

        AlertType.check(0);
        AlertType.check(1);
        AlertType.check(2);
        AlertType.check(3);

        assertThatThrownBy(() -> AlertType.check(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid alert type val: -1");

        assertThatThrownBy(() -> AlertType.check(4))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid alert type val: 4");

        assertThatThrownBy(() -> AlertType.check(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid alert type val: null");
    }

    @Test
    void testCheck2() {
        // check the alert type value must be a power of 2 number
        AlertType[] values = AlertType.values();
        for (int i = 0; i < values.length; i++) {
            int n = values[i].value();
            System.out.println(i + " -> " + n);
            assertThat(n).isGreaterThan(0);
            assertThat(n & (n - 1)).isZero();
            // 校验必须是连续的值：1, 2, 4, 8
            assertThat(n).isEqualTo(LongMath.pow(2, i));
        }
    }

}
