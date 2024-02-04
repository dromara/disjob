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
    public void testConcatSqlLike() {
        Assertions.assertNull(Strings.concatSqlLike(null));
        Assertions.assertTrue(Strings.concatSqlLike("").isEmpty());
        Assertions.assertEquals("^", Strings.concatSqlLike("^"));
        Assertions.assertEquals("$", Strings.concatSqlLike("$"));
        Assertions.assertEquals("^$", Strings.concatSqlLike("^$"));
        Assertions.assertEquals("%$^%", Strings.concatSqlLike("$^"));
        Assertions.assertEquals("%a%", Strings.concatSqlLike("a"));
        Assertions.assertEquals("a%", Strings.concatSqlLike("^a"));
        Assertions.assertEquals("%a", Strings.concatSqlLike("a$"));
    }

    @Test
    public void testTrimUrlPath() {
        Assertions.assertEquals("/test", Strings.trimUrlPath("test///"));
        Assertions.assertEquals("/test/abc", Strings.trimUrlPath("test/abc///"));
        Assertions.assertEquals("/test/abc", Strings.trimUrlPath(" /test/abc/// "));
        Assertions.assertEquals("/", Strings.trimUrlPath(" / // /// "));
        Assertions.assertEquals("/test/abc", Strings.trimUrlPath(" /test/abc/ / / "));
        Assertions.assertEquals("  abc/a / b/ c", "  abc/a / b/ c / /// /".replaceAll("[/\\s]+$", ""));
    }

}
