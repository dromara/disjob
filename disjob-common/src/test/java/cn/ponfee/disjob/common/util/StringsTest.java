/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * Strings test
 *
 * @author Ponfee
 */
public class StringsTest {

    @Test
    public void testWildcardMatch() {
        Assert.assertFalse(Strings.isMatch("aa", "a"));
        Assert.assertTrue(Strings.isMatch("aa", "aa"));
        Assert.assertFalse(Strings.isMatch("aaa", "aa"));
        Assert.assertTrue(Strings.isMatch("aa", "*"));
        Assert.assertTrue(Strings.isMatch("aa", "a*"));
        Assert.assertTrue(Strings.isMatch("ab", "?*"));
        Assert.assertFalse(Strings.isMatch("aab", "c*a*b"));
    }

    @Test
    public void testCount() {
        Assert.assertEquals(Strings.count("aa", "a"), 2);
        Assert.assertEquals(Strings.count("ababa", "aba"), 1);
    }
}
