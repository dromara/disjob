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

package cn.ponfee.disjob.common.util;

import com.google.common.base.CaseFormat;
import org.apache.commons.lang3.StringUtils;
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

        Assertions.assertFalse(StringUtils.containsWhitespace(null));
        Assertions.assertFalse(StringUtils.containsWhitespace(""));
        Assertions.assertTrue(StringUtils.containsWhitespace(" "));
        Assertions.assertTrue(StringUtils.containsWhitespace("a b"));
        Assertions.assertTrue(StringUtils.containsWhitespace("a\nb"));
        Assertions.assertTrue(StringUtils.containsWhitespace("a\tb"));
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
        Assertions.assertEquals("/test", Strings.trimPath("test///"));
        Assertions.assertEquals("/test/abc", Strings.trimPath("test/abc///"));
        Assertions.assertEquals("/test/abc", Strings.trimPath(" /test/abc/// "));
        Assertions.assertEquals("/", Strings.trimPath(" / // /// "));
        Assertions.assertEquals("/", Strings.trimPath("/"));
        Assertions.assertEquals("/", Strings.trimPath(""));
        Assertions.assertEquals("/test/abc", Strings.trimPath(" /test/abc/ / / "));
        Assertions.assertEquals("  abc/a / b/ c", "  abc/a / b/ c / /// /".replaceAll("[/\\s]+$", ""));
        Assertions.assertEquals("/abc", Strings.concatPath("/", "/abc"));
        Assertions.assertEquals("/abc/123", Strings.concatPath(Strings.trimPath(""), "/abc/123"));
        Assertions.assertEquals("/abc/123", Strings.concatPath(Strings.trimPath("/"), "/abc/123"));
        Assertions.assertEquals("/abc/123", Strings.concatPath(Strings.trimPath(""), "/abc/123"));
        Assertions.assertEquals("/abc/123", Strings.concatPath(Strings.trimPath(null), "/abc/123"));
        Assertions.assertEquals("/test/abc/123", Strings.concatPath(Strings.trimPath("/test"), "/abc/123"));
    }

    @Test
    public void testSubstringAfterLast() {
        Assertions.assertNull(Strings.substringAfterLast(null, "."));
        Assertions.assertEquals("", Strings.substringAfterLast("", "."));
        Assertions.assertEquals("abc", Strings.substringAfterLast("abc", "."));
        Assertions.assertEquals("def", Strings.substringAfterLast("abc.def", "."));
        Assertions.assertEquals("abc", Strings.substringAfterLast(".abc", "."));
        Assertions.assertEquals("", Strings.substringAfterLast("abc.", "."));
    }

    @Test
    public void testCaseFormat() {
        Assertions.assertEquals("disjob-admin", CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, "disjob_admin"));
        Assertions.assertEquals("disjob-admin", CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, "disjob-admin"));
        Assertions.assertEquals("disjob", CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, "disjob"));
        Assertions.assertEquals("", CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, ""));
        Assertions.assertEquals(" ", CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, " "));
    }

    @Test
    public void testFormat() {
        Assertions.assertEquals("\t作业ID：1329633560065048576\n", String.format("\t%s%s\n", "作业ID：", 1329633560065048576L));
        Assertions.assertEquals("**作业ID：**1329633560065048576\n", String.format("**%s**%s\n", "作业ID：", 1329633560065048576L));
        Assertions.assertEquals("<b>作业ID：</b>1329633560065048576<br/>", String.format("<b>%s</b>%s<br/>", "作业ID：", 1329633560065048576L));
    }

}
