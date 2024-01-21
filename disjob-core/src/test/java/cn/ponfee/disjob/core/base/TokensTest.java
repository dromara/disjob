/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.base;

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
        String tokenPlain = "tokenPlain";
        String group = "app-test";
        String tokenSecret = Tokens.create(tokenPlain, Tokens.Type.WORKER, Tokens.Mode.AUTHENTICATE, group);
        System.out.println(tokenSecret);
        boolean state = Tokens.verify(tokenSecret, tokenPlain, Tokens.Type.WORKER, Tokens.Mode.AUTHENTICATE, group);
        assertThat(state).isTrue();
    }

}
