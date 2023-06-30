/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Strings test
 *
 * @author Ponfee
 */
public class StringsTest {

    @Test
    public void testWildcardMatch() {
        Assertions.assertFalse(Strings.isMatch("aa", "a"));
        Assertions.assertTrue(Strings.isMatch("aa", "aa"));
        Assertions.assertFalse(Strings.isMatch("aaa", "aa"));
        Assertions.assertTrue(Strings.isMatch("aa", "*"));
        Assertions.assertTrue(Strings.isMatch("aa", "a*"));
        Assertions.assertTrue(Strings.isMatch("ab", "?*"));
        Assertions.assertFalse(Strings.isMatch("aab", "c*a*b"));
    }

    @Test
    public void testCount() {
        Assertions.assertEquals(Strings.count("aa", "a"), 2);
        Assertions.assertEquals(Strings.count("ababa", "aba"), 1);
    }
}
