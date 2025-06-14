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

import cn.ponfee.disjob.common.base.Symbol.Str;
import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.util.List;
import java.util.function.Consumer;

/**
 * String utilities
 *
 * @author Ponfee
 */
public final class Strings {

    private static final List<String> SQL_LIKE_LIST = ImmutableList.of("^", "$", "^$");

    /**
     * <pre>
     * '?' Matches any single character.
     * '*' Matches any sequence of characters (including the empty sequence).
     *
     * isMatch("aa","a")       = false
     * isMatch("aa","aa")      = true
     * isMatch("aaa","aa")     = false
     * isMatch("aa", "*")      = true
     * isMatch("aa", "a*")     = true
     * isMatch("ab", "?*")     = true
     * isMatch("aab", "c*a*b") = false
     * </pre>
     *
     * @param s the text
     * @param p the wildcard pattern
     * @return {@code true} if the string match pattern
     */
    public static boolean isMatch(String s, String p) {
        // 状态 dp[i][j] : 表示 s 的前 i 个字符和 p 的前 j 个字符是否匹配 (true 的话表示匹配)
        // 状态转移方程：
        //      1. 当 s[i] == p[j]，或者 p[j] == ? 那么 dp[i][j] = dp[i - 1][j - 1];
        //      2. 当 p[j] == * 那么 dp[i][j] = dp[i][j - 1] || dp[i - 1][j]    其中：
        //      dp[i][j - 1] 表示 * 代表的是空字符，例如 ab, ab*
        //      dp[i - 1][j] 表示 * 代表的是非空字符，例如 abcd, ab*
        // 初始化：
        //      1. dp[0][0] 表示什么都没有，其值为 true
        //      2. 第一行 dp[0][j]，换句话说，s 为空，与 p 匹配，所以只要 p 开始为 * 才为 true
        //      3. 第一列 dp[i][0]，当然全部为 false
        int m = s.length(), n = p.length();
        boolean[][] dp = new boolean[m + 1][n + 1];
        dp[0][0] = true;
        for (int i = 1; i <= n; ++i) {
            if (p.charAt(i - 1) == '*') {
                dp[0][i] = true;
            } else {
                break;
            }
        }
        for (int i = 1; i <= m; ++i) {
            for (int j = 1; j <= n; ++j) {
                if (p.charAt(j - 1) == '*') {
                    dp[i][j] = dp[i][j - 1] || dp[i - 1][j];
                } else if (p.charAt(j - 1) == '?' || s.charAt(i - 1) == p.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                }
            }
        }
        return dp[m][n];
    }

    /**
     * 驼峰转为带分隔符名字，如驼峰转换为下划线：CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, camelCaseName);
     *
     * @param camelCaseFormat the camel case format
     * @param separator       the separator character
     * @return separator format string
     * @see CaseFormat#to(CaseFormat, String)
     */
    public static String toSeparatedFormat(String camelCaseFormat, char separator) {
        if (StringUtils.isEmpty(camelCaseFormat)) {
            return camelCaseFormat;
        }

        StringBuilder result = new StringBuilder(camelCaseFormat.length() << 1);
        result.append(Character.toLowerCase(camelCaseFormat.charAt(0)));
        for (int i = 1, len = camelCaseFormat.length(); i < len; i++) {
            char ch = camelCaseFormat.charAt(i);
            if (Character.isUpperCase(ch)) {
                result.append(separator).append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    /**
     * 带分隔符名字转驼峰，如下划线转换为驼峰：CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, underscoreName);
     * <pre>
     * 1、LOWER_HYPHEN      ->  连字符的变量命名规范如lower-hyphen
     * 2、LOWER_UNDERSCORE  ->  c++变量命名规范如lower_underscore
     * 3、LOWER_CAMEL       ->  java变量命名规范如lowerCamel
     * 4、UPPER_CAMEL       ->  java和c++类的命名规范如UpperCamel
     * 5、UPPER_UNDERSCORE  ->  java和c++常量的命名规范如UPPER_UNDERSCORE
     * </pre>
     *
     * @param separatedFormat the separated format
     * @param separator       the separator character
     * @return camel case format string
     * @see CaseFormat#to(CaseFormat, String)
     */
    public static String toCamelCaseFormat(String separatedFormat, char separator) {
        if (StringUtils.isEmpty(separatedFormat)) {
            return separatedFormat;
        }

        StringBuilder result = new StringBuilder(separatedFormat.length());
        for (int i = 0, len = separatedFormat.length(); i < len; i++) {
            char ch = separatedFormat.charAt(i);
            if (separator == ch) {
                if (++i < len) {
                    result.append(Character.toUpperCase(separatedFormat.charAt(i)));
                }
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    public static String requireNonBlank(String str) {
        if (StringUtils.isBlank(str)) {
            throw new IllegalArgumentException("Text require non blank.");
        }
        return str;
    }

    public static String concatSqlLike(String str) {
        if (StringUtils.isEmpty(str) || SQL_LIKE_LIST.contains(str)) {
            return str;
        }

        if (str.startsWith("^")) {
            str = str.substring(1);
        } else {
            str = "%" + str;
        }
        if (str.endsWith("$")) {
            str = str.substring(0, str.length() - 1);
        } else {
            str = str + "%";
        }
        return str;
    }

    /**
     * Returns trim path.
     *
     * <pre>
     *  trimPath("test///")          = "/test"
     *  trimPath("test/abc///")      = "/test/abc"
     *  trimPath(" /test/abc/// ")   = "/test/abc"
     *  trimPath(" / // /// ")       = "/"
     *  trimPath(" /test/abc/ / / ") = "/test/abc"
     * </pre>
     *
     * @param path the path
     * @return trim path
     */
    public static String trimPath(String path) {
        if (StringUtils.isBlank(path) || Str.SLASH.equals(path)) {
            return Str.SLASH;
        }

        path = path.replaceAll("[/\\s]+$", "").trim();
        return path.startsWith(Str.SLASH) ? path : Str.SLASH + path;
    }

    /**
     * 拼接两个路径，以“/”开头，不以“/”结尾
     * <p> e.g.: concatPath("/a", "/b/c") -> /a/b/c
     *
     * @param prefixPath the prefix path
     * @param suffixPath the suffix path
     * @return concat path
     */
    public static String concatPath(String prefixPath, String suffixPath) {
        Assert.isTrue(prefixPath.startsWith(Str.SLASH), "Prefix path must start with '/'");
        if (prefixPath.length() > 1) {
            Assert.isTrue(!prefixPath.endsWith(Str.SLASH), "Prefix path cannot end with '/'");
        }
        Assert.isTrue(suffixPath.startsWith(Str.SLASH), "Suffix path must start with '/'");
        if (suffixPath.length() > 1) {
            Assert.isTrue(!suffixPath.endsWith(Str.SLASH), "Suffix path cannot end with '/'");
        }

        if (Str.SLASH.equals(suffixPath)) {
            return prefixPath;
        }
        if (Str.SLASH.equals(prefixPath)) {
            return suffixPath;
        }
        return prefixPath + suffixPath;
    }

    /**
     * <pre>
     *  substringAfterLast(null, ".")      = null
     *  substringAfterLast("", ".")        = ""
     *  substringAfterLast("abc", ".")     = "abc"
     *  substringAfterLast("abc.def", ".") = "def"
     *  substringAfterLast(".abc", ".")    = "abc"
     *  substringAfterLast("abc.", ".")    = ""
     * </pre>
     *
     * @param str       the string
     * @param separator the separator
     * @return the substring after the last occurrence of the separator
     */
    public static String substringAfterLast(String str, String separator) {
        if (StringUtils.isEmpty(str)) {
            return str;
        }
        int lastIndexOf = str.lastIndexOf(separator);
        return lastIndexOf == -1 ? str : str.substring(lastIndexOf + 1);
    }

    public static boolean containsCharOrWhitespace(String str, char chr) {
        if (StringUtils.isEmpty(str)) {
            return false;
        }
        char c;
        for (int len = str.length(), i = 0; i < len; i++) {
            c = str.charAt(i);
            if (c == chr || Character.isWhitespace(c)) {
                return true;
            }
        }
        return false;
    }

    public static void applyIfNotBlank(String str, Consumer<String> consumer) {
        if (StringUtils.isNotBlank(str)) {
            consumer.accept(str.trim());
        }
    }

}
