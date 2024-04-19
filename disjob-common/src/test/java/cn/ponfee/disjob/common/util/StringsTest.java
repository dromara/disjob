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

}
