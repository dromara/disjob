/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.base;

import cn.ponfee.disjob.core.model.TokenType;
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
        String tokenPlain = "1878f0158782423f9306e7d4c70c999c";
        String group = "app-test";
        String tokenSecret = Tokens.createAuthentication(tokenPlain, TokenType.user, group);
        System.out.println(tokenSecret);
        boolean state = Tokens.verifyAuthentication(tokenSecret, tokenPlain, TokenType.user, group);
        assertThat(state).isTrue();
    }

}
