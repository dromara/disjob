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

package cn.ponfee.disjob.core.base;

import cn.ponfee.disjob.core.enums.TokenType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Token test
 *
 * @author Ponfee
 */
public class TokensTest {

    @Test
    public void test() {
        String tokenKey = "1878f0158782423f9306e7d4c70c999c";
        String group = "app-test";
        String tokenSecret = Tokens.createAuthentication(tokenKey, TokenType.user, group);
        System.out.println(tokenSecret);
        assertThat(Tokens.verifyAuthentication(tokenSecret, tokenKey, TokenType.user, group)).isTrue();
    }

}
