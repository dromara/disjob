/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import com.google.common.base.CaseFormat;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * String utilities
 *
 * @author Ponfee
 */
public final class Strings {

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
     * @param camelcaseName the camelcase name
     * @param separator     the separator
     * @return with separator name
     * @see CaseFormat#to(CaseFormat, String)
     */
    public static String toSeparatedName(String camelcaseName, char separator) {
        if (StringUtils.isEmpty(camelcaseName)) {
            return camelcaseName;
        }

        StringBuilder result = new StringBuilder(camelcaseName.length() << 1);
        result.append(Character.toLowerCase(camelcaseName.charAt(0)));
        for (int i = 1, len = camelcaseName.length(); i < len; i++) {
            char ch = camelcaseName.charAt(i);
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
     * 1、LOWER_HYPHEN      ->  连字符的变量命名规范如lower-hyphen
     * 2、LOWER_UNDERSCORE  ->  c++变量命名规范如lower_underscore
     * 3、LOWER_CAMEL       ->  java变量命名规范如lowerCamel
     * 4、UPPER_CAMEL       ->  java和c++类的命名规范如UpperCamel
     * 5、UPPER_UNDERSCORE  ->  java和c++常量的命名规范如UPPER_UNDERSCORE
     *
     * @param separatedName the separated name
     * @param separator     the separator
     * @return camelcase name
     * @see CaseFormat#to(CaseFormat, String)
     */
    public static String toCamelcaseName(String separatedName, char separator) {
        if (StringUtils.isEmpty(separatedName)) {
            return separatedName;
        }

        StringBuilder result = new StringBuilder(separatedName.length());
        for (int i = 0, len = separatedName.length(); i < len; i++) {
            char ch = separatedName.charAt(i);
            if (separator == ch) {
                if (++i < len) {
                    result.append(Character.toUpperCase(separatedName.charAt(i)));
                }
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    public static boolean containsAny(String str, List<String> searches) {
        if (StringUtils.isEmpty(str) || CollectionUtils.isEmpty(searches)) {
            return false;
        }
        for (String search : searches) {
            if (StringUtils.contains(str, search)) {
                return true;
            }
        }
        return false;
    }

    public static String requireNonBlank(String str) {
        if (StringUtils.isBlank(str)) {
            throw new IllegalArgumentException("Text require non blank.");
        }
        return str;
    }
}
